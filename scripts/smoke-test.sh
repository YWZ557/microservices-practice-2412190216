#!/usr/bin/env bash
# 第 06 周冒烟测试：健康检查 + 跨服务借还书主流程 + 失败场景
# 运行环境：Git Bash / WSL / Linux 均可，只需要 bash + curl
# 说明：数据校验优先走 HTTP 接口，docker 可用时再额外做 SQL 交叉校验
# 用法：
#   bash scripts/smoke-test.sh                # 主流程 + 资源不存在
#   WITH_OUTAGE=1 bash scripts/smoke-test.sh  # 追加“图书服务不可用”场景（需要 docker）
set -uo pipefail

cd "$(dirname "$0")/.."

USER_URL=${USER_URL:-http://localhost:8081}
BOOK_URL=${BOOK_URL:-http://localhost:8082}
BORROW_URL=${BORROW_URL:-http://localhost:8083}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-root123456}
WITH_OUTAGE=${WITH_OUTAGE:-0}

PASS=0
FAIL=0
SKIP=0
TMP_BODY=$(mktemp 2>/dev/null || echo "./.smoke_body.tmp")

# ---------- 通用函数 ----------
json_num() { printf '%s' "$1" | grep -o "\"$2\":[0-9]\+" | head -n1 | cut -d: -f2; }
json_str() { printf '%s' "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -n1 | cut -d'"' -f4; }
json_health() { printf '%s' "$1" | grep -o '"status":"[A-Z]*"' | tail -n1 | cut -d'"' -f4; }
contains() { case "$1" in *"$2"*) echo yes ;; *) echo no ;; esac; }

# 探测 docker：WSL 里的 bash 常常找不到 docker，找不到就退化到纯接口校验
DOCKER=""
if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  DOCKER="docker"
elif command -v docker.exe >/dev/null 2>&1 && docker.exe compose version >/dev/null 2>&1; then
  DOCKER="docker.exe"
fi

mysql_q() {
  [ -z "$DOCKER" ] && return 1
  $DOCKER compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "$1" 2>/dev/null | tr -d '\r'
}

HTTP_CODE=""
BODY=""
LAST_REQ=""

# req 方法 路径 [token] [json]
req() {
  local method=$1 uri=$2 token=${3:-} data=${4:-}
  local args=(-s -m 10 -o "$TMP_BODY" -w '%{http_code}' -X "$method" "$uri")
  [ -n "$token" ] && args+=(-H "token: $token")
  [ -n "$data" ] && args+=(-H 'Content-Type: application/json' -d "$data")
  HTTP_CODE=$(curl "${args[@]}")
  BODY=$(cat "$TMP_BODY" 2>/dev/null)
  LAST_REQ="$method $(printf '%s' "$uri" | sed 's#http://[^/]*##')"
  printf '%s\n' "$LAST_REQ" >/dev/null
}

check() { # 描述 实际 期望
  if [ "$2" = "$3" ]; then
    PASS=$((PASS + 1)); printf '  [PASS] %-28s 实际=%s\n' "$1" "$2"
  else
    FAIL=$((FAIL + 1)); printf '  [FAIL] %-28s 实际=%s 期望=%s\n' "$1" "$2" "$3"
  fi
}

skip() { SKIP=$((SKIP + 1)); printf '  [SKIP] %s\n' "$1"; }

# 数据库交叉校验：docker 不可用时跳过，不算失败
db_check() { # 描述 SQL 期望
  if [ -z "$DOCKER" ]; then skip "$1（无 docker CLI，已由接口校验覆盖）"; return; fi
  check "$1" "$(mysql_q "$2")" "$3"
}

borrow_total() { # 当前读者借阅记录总数
  req POST "$BORROW_URL/api/reader/borrow/page" "$TOKEN" '{"page":1,"pageSize":1}'
  json_num "$BODY" total
}

book_status() { # 图书当前状态
  req GET "$BOOK_URL/api/reader/book/$1" "$TOKEN"
  json_str "$BODY" status
}

record_status() { # 指定图书最新一条借阅记录状态
  req POST "$BORROW_URL/api/reader/borrow/page" "$TOKEN" '{"page":1,"pageSize":50}'
  printf '%s' "$BODY" | sed 's/},{/}\n{/g' | grep "\"bookId\":$1" | grep -o '"status":"[A-Z]*"' | tail -n1 | cut -d'"' -f4
}

record_id_of() { # 指定图书最新一条借阅记录 id
  req POST "$BORROW_URL/api/reader/borrow/page" "$TOKEN" '{"page":1,"pageSize":50}'
  printf '%s' "$BODY" | sed 's/},{/}\n{/g' | grep "\"bookId\":$1" \
    | grep -o '"id":[0-9]*' | cut -d: -f2 | sort -n | tail -n1
}

cleanup() { [ -f "$TMP_BODY" ] && rm -f "$TMP_BODY"; }
trap cleanup EXIT

echo "=== 1. 健康检查（每个服务 + MySQL） ==="
for pair in "user-service|$USER_URL" "book-service|$BOOK_URL" "borrow-service|$BORROW_URL"; do
  name=${pair%%|*}; url=${pair##*|}
  req GET "$url/actuator/health"
  printf '  %-15s %s HTTP=%s status=%s\n' "$name" "$url" "$HTTP_CODE" "$(json_health "$BODY")"
  check "$name HTTP 200" "$HTTP_CODE" "200"
  check "$name health UP" "$(json_health "$BODY")" "UP"
  check "$name db UP" "$(contains "$(printf '%s' "$BODY" | tr -d '\n')" '"db":{"details"')" "yes"
done
if [ -z "$DOCKER" ]; then
  skip "mysql 容器健康（当前 bash 找不到 docker，可直接看 docker compose ps）"
else
  check "mysql 容器健康" "$($DOCKER compose ps mysql --format '{{.Health}}' 2>/dev/null | tr -d '\r')" "healthy"
fi

echo
echo "=== 2. 读者登录 ==="
req POST "$USER_URL/api/reader/login" "" '{"username":"xiaoming","password":"123456"}'
TOKEN=$(json_str "$BODY" data)
check "读者登录 code" "$(json_num "$BODY" code)" "1"
check "返回 token" "$([ -n "$TOKEN" ] && echo yes || echo no)" "yes"

req POST "$USER_URL/api/admin/login" "" '{"username":"admin","password":"123456"}'
ADMIN_TOKEN=$(json_str "$BODY" data)
check "管理员登录 code" "$(json_num "$BODY" code)" "1"

echo
echo "=== 3. 跨服务借书 -> 还书主流程 ==="
BOOK_ID=""
for id in 1 2 3 4 5 6 7 8; do
  if [ "$(book_status "$id")" = "AVAILABLE" ]; then BOOK_ID=$id; break; fi
done
if [ -z "$BOOK_ID" ]; then echo "  [ABORT] 1-8 号图书中没有 AVAILABLE 状态，无法继续"; exit 1; fi
DUE_DATE=$(date -d '+30 days' +%Y-%m-%d 2>/dev/null || date -v+30d +%Y-%m-%d)
TOTAL_BEFORE=$(borrow_total)
echo "  目标图书 bookId=$BOOK_ID，借阅前记录数=$TOTAL_BEFORE"

req POST "$BORROW_URL/api/reader/borrow" "$TOKEN" "{\"bookId\":$BOOK_ID,\"dueDate\":\"$DUE_DATE\"}"
printf '  %-20s HTTP=%s code=%s msg=%s\n' "$LAST_REQ" "$HTTP_CODE" "$(json_num "$BODY" code)" "$(json_str "$BODY" msg)"
check "借书 HTTP 200" "$HTTP_CODE" "200"
check "借书 code" "$(json_num "$BODY" code)" "1"
check "图书变为 BORROWED" "$(book_status "$BOOK_ID")" "BORROWED"
check "接口记录数+1" "$(borrow_total)" "$((TOTAL_BEFORE + 1))"
db_check "库中记录数+1" "SELECT COUNT(*) FROM library_borrow.borrow_record;" "$((TOTAL_BEFORE + 1))"

RECORD_ID=$(record_id_of "$BOOK_ID")
check "取到新记录 id" "$([ -n "$RECORD_ID" ] && echo yes || echo no)" "yes"

req GET "$BORROW_URL/api/reader/borrow/return/$RECORD_ID" "$TOKEN"
printf '  %-20s HTTP=%s code=%s msg=%s\n' "$LAST_REQ" "$HTTP_CODE" "$(json_num "$BODY" code)" "$(json_str "$BODY" msg)"
check "还书 code" "$(json_num "$BODY" code)" "1"
check "图书恢复 AVAILABLE" "$(book_status "$BOOK_ID")" "AVAILABLE"
check "记录变为 RETURNED" "$(record_status "$BOOK_ID")" "RETURNED"
db_check "库中图书 AVAILABLE" "SELECT status FROM library_book.book WHERE id=$BOOK_ID;" "AVAILABLE"

echo
echo "=== 4. 失败场景：依赖资源不存在 ==="
TOTAL_BEFORE_FAIL=$(borrow_total)
req POST "$BORROW_URL/api/reader/borrow" "$TOKEN" '{"bookId":9999,"dueDate":"2026-12-31"}'
printf '  %-20s HTTP=%s code=%s msg=%s\n' "$LAST_REQ" "$HTTP_CODE" "$(json_num "$BODY" code)" "$(json_str "$BODY" msg)"
check "不存在图书 code" "$(json_num "$BODY" code)" "0"
check "不存在图书提示" "$(contains "$BODY" "图书不存在")" "yes"
check "未写借阅记录" "$(borrow_total)" "$TOTAL_BEFORE_FAIL"
db_check "库中记录数不变" "SELECT COUNT(*) FROM library_borrow.borrow_record;" "$TOTAL_BEFORE_FAIL"

if [ -n "$ADMIN_TOKEN" ]; then
  req POST "$BORROW_URL/api/admin/borrow" "$ADMIN_TOKEN" "{\"bookId\":$BOOK_ID,\"readerId\":9999,\"dueDate\":\"2026-12-31\"}"
  printf '  %-20s HTTP=%s code=%s msg=%s\n' "$LAST_REQ" "$HTTP_CODE" "$(json_num "$BODY" code)" "$(json_str "$BODY" msg)"
  check "不存在读者 code" "$(json_num "$BODY" code)" "0"
  check "不存在读者提示" "$(contains "$BODY" "读者不存在")" "yes"
  check "图书状态未变" "$(book_status "$BOOK_ID")" "AVAILABLE"
else
  skip "不存在读者场景（未取到管理员 token）"
fi

echo
echo "=== 5. 失败场景：依赖服务不可用（可选） ==="
if [ "$WITH_OUTAGE" != "1" ]; then
  skip "未设置 WITH_OUTAGE=1"
elif [ -z "$DOCKER" ]; then
  skip "需要 docker 才能停止 book-service"
else
  echo "  停止 book-service ..."
  $DOCKER compose stop book-service >/dev/null 2>&1
  sleep 3
  TOTAL_BEFORE_OUTAGE=$(borrow_total)
  req POST "$BORROW_URL/api/reader/borrow" "$TOKEN" "{\"bookId\":$BOOK_ID,\"dueDate\":\"$DUE_DATE\"}"
  printf '  %-20s HTTP=%s code=%s msg=%s\n' "$LAST_REQ" "$HTTP_CODE" "$(json_num "$BODY" code)" "$(json_str "$BODY" msg)"
  check "依赖不可用 code" "$(json_num "$BODY" code)" "0"
  check "依赖不可用提示" "$(contains "$BODY" "图书服务不可用")" "yes"
  check "未写借阅记录" "$(borrow_total)" "$TOTAL_BEFORE_OUTAGE"
  echo "  恢复 book-service ..."
  $DOCKER compose start book-service >/dev/null 2>&1
  for i in $(seq 1 30); do
    sleep 2
    req GET "$BOOK_URL/actuator/health"
    [ "$(json_health "$BODY")" = "UP" ] && break
  done
  check "book-service 已恢复" "$(json_health "$BODY")" "UP"
fi

echo
echo "=== 6. 清理测试数据 ==="
if [ -n "$RECORD_ID" ]; then
  req DELETE "$BORROW_URL/api/reader/borrow/$RECORD_ID" "$TOKEN"
  check "删除测试记录 code" "$(json_num "$BODY" code)" "1"
fi
check "记录数恢复基线" "$(borrow_total)" "$TOTAL_BEFORE"
check "图书状态 AVAILABLE" "$(book_status "$BOOK_ID")" "AVAILABLE"

echo
echo "======== 结果：PASS=$PASS FAIL=$FAIL SKIP=$SKIP ========"
[ "$FAIL" -eq 0 ] || exit 1
