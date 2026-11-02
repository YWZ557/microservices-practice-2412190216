#!/usr/bin/env bash
# 冒烟检查：Nacos 控制台可达性 + 各服务 Actuator 健康端点
# 在仓库根目录执行： bash scripts/smoke-test.sh
# 依赖：curl、bash（Windows 请在 Git Bash 中运行）
set -uo pipefail

NACOS_CONSOLE_URL="${NACOS_CONSOLE_URL:-http://127.0.0.1:18080}"
TIMEOUT="${TIMEOUT:-5}"

failures=0
pass() { printf '[PASS] %s\n' "$1"; }
fail() { printf '[FAIL] %s\n' "$1"; failures=$((failures + 1)); }

# --- 前置校验：控制台地址不能是旧版 8848/nacos ---
case "$NACOS_CONSOLE_URL" in
  *:8848*|*/nacos*)
    printf '[FAIL] 控制台地址配置错误: %s\n' "$NACOS_CONSOLE_URL"
    printf '       Nacos 3 控制台端口为 8080（宿主机映射如 18080），不是 8848/nacos\n'
    exit 2
    ;;
esac

# --- 1. Nacos 控制台 ---
echo "==> [1/2] Nacos 控制台: $NACOS_CONSOLE_URL"
code="$(curl -s -o /dev/null -w '%{http_code}' \
  --connect-timeout "$TIMEOUT" --max-time "$TIMEOUT" \
  "$NACOS_CONSOLE_URL/" 2>/dev/null || true)"
if [[ "$code" =~ ^(200|301|302|303)$ ]]; then
  pass "Nacos 控制台可访问 (HTTP $code)"
else
  fail "Nacos 控制台不可访问 (HTTP ${code:-无响应})"
fi

# --- 2. 健康端点 ---
check_health() {
  local name="$1" url="$2" code tmp body normalized
  tmp="$(mktemp)"
  code="$(curl -s -o "$tmp" -w '%{http_code}' \
    --connect-timeout "$TIMEOUT" --max-time "$TIMEOUT" "$url" 2>/dev/null || true)"
  body="$(cat "$tmp" 2>/dev/null)"
  rm -f "$tmp"

  if [[ "$code" != "200" ]]; then
    case "$code" in
      401) fail "$name 返回 401（网关未放行 /actuator/**）" ;;
      404) fail "$name 返回 404（未引入 actuator 或未暴露 health）" ;;
      000|"") fail "$name 连接失败（服务未启动或端口不通）" ;;
      *)   fail "$name 返回 HTTP $code" ;;
    esac
    return
  fi

  # 去掉空格、制表、换行后再判断，兼容 "status" : "UP" 与 "status":"UP"
  normalized="$(printf '%s' "$body" | tr -d ' \t\r\n')"
  if [[ "$normalized" == *'"status":"UP"'* && "$normalized" != *'"status":"DOWN"'* ]]; then
    pass "$name 健康 ($url)"
  else
    fail "$name 状态异常：$(printf '%s' "$body" | tr -d '\n' | cut -c1-120)"
  fi
}


echo
echo "==> [2/2] 服务健康端点"
check_health "library-gateway" "http://127.0.0.1:8080/actuator/health"
check_health "library-user"    "http://127.0.0.1:8081/actuator/health"
check_health "library-book"    "http://127.0.0.1:8082/actuator/health"
check_health "library-borrow"  "http://127.0.0.1:8083/actuator/health"

# --- 汇总 ---
echo
if (( failures > 0 )); then
  printf '检查未通过：%d 项失败\n' "$failures"
  exit 1
fi
printf '全部检查通过\n'
exit 0
