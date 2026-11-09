#!/usr/bin/env bash
# ============================================================
# 跨服务端到端验证：借书流程
# ------------------------------------------------------------
# 覆盖：正常借书 / 重复借阅 / 图书不存在 / 依赖不可用 / 依赖恢复
# 用法：bash scripts/e2e-test.sh
#       SKIP_DOWN=1 bash scripts/e2e-test.sh   # 跳过停服务场景
# 前置：docker compose up -d --build
# 说明：写操作超时后结果可能未知，本脚本只验证"快速失败不被当成成功"，
#       不对超时后的部分成功做断言。
# ============================================================
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR" || exit 1

BORROW_BASE="${BORROW_BASE:-http://localhost:8083}"
USER_BASE="${USER_BASE:-http://localhost:8081}"
BOOK_BASE="${BOOK_BASE:-http://localhost:8082}"
READER_USER="${READER_USER:-reader01}"
READER_PASS="${READER_PASS:-123456}"
BOOK_ID="${BOOK_ID:-1}"
DUE_DATE="${DUE_DATE:-2026-12-31}"

PASS=0
FAIL=0

pass() { printf '  [PASS] %s\n' "$1"; PASS=$((PASS + 1)); }
fail() { printf '  [FAIL] %s\n' "$1"; FAIL=$((FAIL + 1)); }
step() { printf '\n==> %s\n' "$1"; }

# 取 JSON 第一层字段，兼容字符串与数字
extract() {
    printf '%s' "$1" | tr -d '\r\n' |
        sed -n "s/.*\"$2\"[[:space:]]*:[[:space:]]*\"\{0,1\}\([^\",}]*\)\"\{0,1\}.*/\1/p"
}

api() {
    local method=$1 url=$2 data=${3:-} token=${4:-}
    local args=(-s -w '\n%{http_code}' -X "$method" "$url")
    [ -n "$data" ] && args+=(-H 'Content-Type: application/json' -d "$data")
    [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
    curl "${args[@]}"
}

parse() {
    HTTP_CODE=$(printf '%s' "$1" | tail -n1)
    BODY=$(printf '%s' "$1" | sed '$d')
}

step "0. 重置测试数据"
docker compose exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root123456}" -e "
UPDATE library_book.book SET status='AVAILABLE' WHERE id=${BOOK_ID};
DELETE FROM library_borrow.borrow_record WHERE book_id=${BOOK_ID};
" >/dev/null 2>&1 && echo "  book ${BOOK_ID} 已重置" || echo "  跳过（mysql 不可达）"

step "1. 读者登录"
parse "$(api POST "$USER_BASE/api/reader/login" "{\"username\":\"$READER_USER\",\"password\":\"$READER_PASS\"}")"
TOKEN=$(extract "$BODY" data)
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
    pass "登录成功"
else
    fail "登录失败：$BODY"
    exit 1
fi

step "2. 正常借书"
parse "$(api POST "$BORROW_BASE/api/reader/borrow" "{\"bookId\":$BOOK_ID,\"dueDate\":\"$DUE_DATE\"}" "$TOKEN")"
[ "$(extract "$BODY" code)" = "1" ] && pass "借书成功" || fail "借书失败：$BODY"

step "3. 校验图书状态被下游修改"
parse "$(api GET "$BOOK_BASE/internal/books/$BOOK_ID")"
[ "$(extract "$BODY" status)" = "BORROWED" ] && pass "book.status=BORROWED" || fail "状态未变更：$BODY"

step "4. 重复借阅应被拒绝"
parse "$(api POST "$BORROW_BASE/api/reader/borrow" "{\"bookId\":$BOOK_ID,\"dueDate\":\"$DUE_DATE\"}" "$TOKEN")"
if [ "$(extract "$BODY" code)" = "0" ] && printf '%s' "$BODY" | grep -q "无可借库存"; then
    pass "重复借阅被拒绝"
else
    fail "重复借阅未拒绝：$BODY"
fi

step "5. 借不存在的图书应返回明确错误"
parse "$(api POST "$BORROW_BASE/api/reader/borrow" '{"bookId":999999,"dueDate":"2026-12-31"}' "$TOKEN")"
if [ "$(extract "$BODY" code)" = "0" ] && printf '%s' "$BODY" | grep -q "图书不存在"; then
    pass "不存在图书被拒绝"
else
    fail "异常图书未拒绝：$BODY"
fi

if [ "${SKIP_DOWN:-0}" != "1" ]; then
    step "6. 停止 book-service 后借书"
    docker compose stop book-service >/dev/null 2>&1
    sleep 3
    parse "$(api POST "$BORROW_BASE/api/reader/borrow" "{\"bookId\":$BOOK_ID,\"dueDate\":\"$DUE_DATE\"}" "$TOKEN")"
    echo "  响应：$BODY"
    if [ "$(extract "$BODY" code)" = "0" ] && printf '%s' "$BODY" | grep -q "不可用"; then
        pass "依赖不可用返回失败，未误判为成功"
    elif [ "$(extract "$BODY" code)" = "1" ]; then
        fail "依赖不可用被误判为成功（严重）"
    else
        fail "依赖不可用处理异常：$BODY"
    fi

    step "7. 恢复 book-service"
    docker compose start book-service >/dev/null 2>&1
    for _ in $(seq 1 30); do
        curl -s -o /dev/null "$BOOK_BASE/internal/books/$BOOK_ID" && break
        sleep 2
    done
    parse "$(api GET "$BOOK_BASE/internal/books/$BOOK_ID")"
    [ "$HTTP_CODE" = "200" ] && pass "服务恢复，查询可用" || fail "恢复失败：HTTP $HTTP_CODE"
fi

echo
echo "PASS=$PASS FAIL=$FAIL"
[ "$FAIL" -eq 0 ]
