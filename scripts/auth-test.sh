#!/usr/bin/env bash
# ============================================================
# 网关鉴权验证脚本
# ------------------------------------------------------------
# 覆盖：
#   1. 未登录访问受保护接口，应失败（401）
#   2. 登录获取 Token
#   3. 携带 Token 访问受保护接口，应成功（200）
#   4. 使用低权限 Token 访问管理接口，应失败（403）
#   5. 对照组：管理员 Token 访问管理接口，应成功（200）
#
# 用法：bash scripts/auth-test.sh
# 前置：gateway 8080、user 8081、book 8082 已启动
# 环境变量：BASE / READER_USER / READER_PASS / ADMIN_USER / ADMIN_PASS
# ============================================================
set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
READER_USER="${READER_USER:-test_reader01}"
READER_PASS="${READER_PASS:-123456}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-123456}"

PASS=0
FAIL=0

pass() { printf '  [PASS] %s\n' "$1"; PASS=$((PASS + 1)); }
fail() { printf '  [FAIL] %s\n' "$1"; FAIL=$((FAIL + 1)); }
step() { printf '\n==> %s\n' "$1"; }

# 从 JSON 中取第一层字段，兼容字符串与数字
extract() {
    printf '%s' "$1" | tr -d '\r\n' |
        sed -n "s/.*\"$2\"[[:space:]]*:[[:space:]]*\"\{0,1\}\([^\",}]*\)\"\{0,1\}.*/\1/p"
}

BODY_FILE="$(mktemp)"
trap 'rm -f "$BODY_FILE"' EXIT

# 输出 HTTP 状态码，响应体写入 $BODY_FILE
request() {
    local method=$1 path=$2 data=${3:-} token=${4:-}
    local args=(-s -o "$BODY_FILE" -w '%{http_code}' -X "$method" "$BASE$path")
    [ -n "$data" ] && args+=(-H 'Content-Type: application/json' -d "$data")
    [ -n "$token" ] && args+=(-H "Authorization: $token")
    curl "${args[@]}"
}

BODY() { cat "$BODY_FILE" 2>/dev/null; }

printf 'BASE=%s\n' "$BASE"
printf 'READER=%s  ADMIN=%s\n' "$READER_USER" "$ADMIN_USER"

# ------------------------------------------------------------
step "1. 未登录访问受保护接口，应失败"
CODE=$(request POST /book/reader/page '{"page":1,"pageSize":3}')
if [ "$CODE" = "401" ]; then
    pass "无 Token 访问 /book/reader/page 返回 401"
else
    fail "期望 401，实际 $CODE：$(BODY)"
fi

# ------------------------------------------------------------
step "2. 读者登录获取 Token"
CODE=$(request POST /reader/login "{\"username\":\"$READER_USER\",\"password\":\"$READER_PASS\"}")
READER_TOKEN=$(extract "$(BODY)" data)
if [ "$CODE" = "200" ] && [ -n "$READER_TOKEN" ] && [ "$READER_TOKEN" != "null" ]; then
    pass "读者登录成功，Token 长度 ${#READER_TOKEN}"
else
    fail "读者登录失败：HTTP $CODE $(BODY)"
    printf '\nPASS=%d FAIL=%d\n' "$PASS" "$FAIL"
    exit 1
fi

# ------------------------------------------------------------
step "3. 携带 Token 访问受保护接口，应成功"
CODE=$(request POST /book/reader/page '{"page":1,"pageSize":3}' "$READER_TOKEN")
if [ "$CODE" = "200" ] && [ "$(extract "$(BODY)" code)" = "1" ]; then
    pass "读者 Token 访问 /book/reader/page 返回 200 且 code=1"
else
    fail "期望 200，实际 $CODE：$(BODY)"
fi

# ------------------------------------------------------------
step "4. 低权限 Token 访问管理接口，应失败"
CODE=$(request POST /user/admin/page '{"page":1,"pageSize":3}' "$READER_TOKEN")
if [ "$CODE" = "403" ]; then
    pass "读者 Token 访问 /user/admin/page 返回 403，越权被拒"
else
    fail "期望 403，实际 $CODE：$(BODY)"
fi

# ------------------------------------------------------------
step "5. 对照组：管理员 Token 访问管理接口，应成功"
CODE=$(request POST /admin/login "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}")
ADMIN_TOKEN=$(extract "$(BODY)" data)
if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
    fail "管理员登录失败：HTTP $CODE $(BODY)"
else
    CODE=$(request POST /user/admin/page '{"page":1,"pageSize":3}' "$ADMIN_TOKEN")
    if [ "$CODE" = "200" ] && [ "$(extract "$(BODY)" code)" = "1" ]; then
        pass "管理员 Token 访问 /user/admin/page 返回 200 且 code=1"
    else
        fail "期望 200，实际 $CODE：$(BODY)"
    fi
fi

printf '\nPASS=%d FAIL=%d\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
