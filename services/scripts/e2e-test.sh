#!/usr/bin/env bash
# ============================================================
# Monolith（Feign + Nacos）端到端验证
# 入口：gateway 8080
# 覆盖：正常借书 / 重复借阅 / 图书不存在 / book-service 停机 / 恢复
# 每个失败场景都断言 library_borrow.borrow_record 与 library_book.book 的前后状态
# 用法：cd services && bash scripts/e2e-test.sh
#       SKIP_DOWN=1 bash scripts/e2e-test.sh
# ============================================================
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR" || exit 1

BASE="${BASE:-http://localhost:8080}"
BOOK_ID="${BOOK_ID:-4}"
READER_ID="${READER_ID:-2}"
DUE_DATE="${DUE_DATE:-2026-12-31}"
SKIP_DOWN="${SKIP_DOWN:-0}"

# 只从 .env 读取 MySQL 密码，避免把带 <...> 占位符的整份 .env source 进来
if [ -f .env ]; then
  env_pwd="$(sed -n 's/^MYSQL_ROOT_PASSWORD=//p' .env | tail -n 1)"
  [ -n "$env_pwd" ] && MYSQL_ROOT_PASSWORD="$env_pwd"
fi
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root123456}"

PASS=0
FAIL=0
pass() { printf '  [PASS] %s\n' "$1"; PASS=$((PASS + 1)); }
fail() { printf '  [FAIL] %s\n' "$1"; FAIL=$((FAIL + 1)); }
step() { printf '\n==> %s\n' "$1"; }

extract() {
  printf '%s' "$1" | tr -d '\r\n' |
    sed -n "s/.*\"$2\"[[:space:]]*:[[:space:]]*\"\{0,1\}\([^\",}]*\)\"\{0,1\}.*/\1/p"
}

api() {
  local method=$1 url=$2 data=${3:-} token=${4:-}
  local args=(-s -w '\n%{http_code}' -X "$method" "$url")
  [ -n "$data" ] && args+=(-H 'Content-Type: application/json' -d "$data")
  [ -n "$token" ] && args+=(-H "Authorization: $token")
  curl "${args[@]}"
}

parse() {
  HTTP_CODE=$(printf '%s' "$1" | tail -n 1)
  BODY=$(printf '%s' "$1" | sed '$d')
}

sql() {
  docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -B -e "$1" 2>/dev/null
}

reset_book() {
  sql "UPDATE library_book.book SET status='AVAILABLE' WHERE id=$BOOK_ID;
       DELETE FROM library_borrow.borrow_record WHERE book_id=$BOOK_ID;" >/dev/null
}

count_records() {
  sql "SELECT COUNT(*) FROM library_borrow.borrow_record WHERE book_id=$BOOK_ID;"
}

book_status() {
  sql "SELECT status FROM library_book.book WHERE id=$BOOK_ID;"
}

expect_db() {
  local label=$1 want_count=$2 want_status=$3
  local c s
  c="$(count_records)"
  s="$(book_status)"
  if [ "$c" = "$want_count" ] && [ "$s" = "$want_status" ]; then
    pass "$label：记录数=$c，book.status=$s"
  else
    fail "$label：期望记录数=$want_count/status=$want_status，实际记录数=$c/status=$s"
  fi
}

step "0. 重置测试数据（book_id=$BOOK_ID）"
reset_book
expect_db "重置后基线" 0 AVAILABLE

step "1. 管理员登录"
parse "$(api POST "$BASE/admin/login" '{"username":"admin","password":"123456"}')"
TOKEN="$(extract "$BODY" data)"
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  pass "登录成功"
else
  fail "登录失败：$BODY"
  exit 1
fi

step "2. 正常借书"
parse "$(api POST "$BASE/borrow/admin" "{\"bookId\":$BOOK_ID,\"readerId\":$READER_ID,\"dueDate\":\"$DUE_DATE\"}" "$TOKEN")"
if [ "$(extract "$BODY" code)" = "1" ]; then
  pass "借书成功 code=1"
else
  fail "借书失败：$BODY"
fi
expect_db "借书后" 1 BORROWED

step "3. 重复借阅应被拒绝"
parse "$(api POST "$BASE/borrow/admin" "{\"bookId\":$BOOK_ID,\"readerId\":$READER_ID,\"dueDate\":\"$DUE_DATE\"}" "$TOKEN")"
if [ "$(extract "$BODY" code)" = "0" ] && printf '%s' "$BODY" | grep -q "无可借库存"; then
  pass "重复借阅被拒绝"
else
  fail "重复借阅未拒绝：$BODY"
fi
expect_db "重复借阅后数据不变" 1 BORROWED

step "4. 借不存在的图书应返回明确错误"
parse "$(api POST "$BASE/borrow/admin" '{"bookId":999999,"readerId":2,"dueDate":"2026-12-31"}' "$TOKEN")"
if [ "$(extract "$BODY" code)" = "0" ] && printf '%s' "$BODY" | grep -q "图书不存在"; then
  pass "不存在的图书被拒绝"
else
  fail "异常图书未拒绝：$BODY"
fi
missing_count="$(sql "SELECT COUNT(*) FROM library_borrow.borrow_record WHERE book_id=999999;")"
if [ "$missing_count" = "0" ]; then
  pass "不存在的图书无借阅记录"
else
  fail "不存在的图书产生了 $missing_count 条记录"
fi

if [ "$SKIP_DOWN" != "1" ]; then
  step "5. 停止 book-service 后借书"
  reset_book
  expect_db "停服务前基线" 0 AVAILABLE
  docker compose stop book-service >/dev/null 2>&1
  sleep 5
  parse "$(api POST "$BASE/borrow/admin" "{\"bookId\":$BOOK_ID,\"readerId\":$READER_ID,\"dueDate\":\"$DUE_DATE\"}" "$TOKEN")"
  echo "  响应：$BODY"
  if [ "$(extract "$BODY" code)" = "0" ] && printf '%s' "$BODY" | grep -q "不可用"; then
    pass "依赖不可用返回业务失败，未误判为成功"
  elif [ "$(extract "$BODY" code)" = "1" ]; then
    fail "依赖不可用被误判为成功（严重）"
  else
    fail "依赖不可用处理异常：$BODY"
  fi
  expect_db "依赖不可用后数据不变" 0 AVAILABLE

  step "6. 恢复 book-service 后重试"
  docker compose start book-service >/dev/null 2>&1
  for _ in $(seq 1 30); do
    code="$(curl -s -o /dev/null -w '%{http_code}' \
      -H "Authorization: $TOKEN" "$BASE/book/admin/$BOOK_ID")"
    [ "$code" = "200" ] && break
    sleep 2
  done
  parse "$(api POST "$BASE/borrow/admin" "{\"bookId\":$BOOK_ID,\"readerId\":$READER_ID,\"dueDate\":\"$DUE_DATE\"}" "$TOKEN")"
  if [ "$(extract "$BODY" code)" = "1" ]; then
    pass "恢复后借书成功"
  else
    fail "恢复后借书失败：$BODY"
  fi
  expect_db "恢复后" 1 BORROWED
fi

echo
echo "PASS=$PASS FAIL=$FAIL"
[ "$FAIL" -eq 0 ]
