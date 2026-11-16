# Week 09 · API Gateway 与认证服务

| 项 | 内容 |
| --- | --- |
| 周次 | 第 09 周 |
| 主题 | API Gateway 统一入口 + 认证服务（登录、JWT、注册） |
| 实现模块 | `services/library-gateway`、`services/library-user` |
| 运行环境 | Windows 11 + JDK 25 + Docker Compose + Nacos 3.2.4 + MySQL 8 |
| 网关端口 | 8080 |
| 完成时间 | 2026-11-16 |

---

## 1. 实现思路

### 1.1 统一入口

外部请求不再直连 8081/8082/8083，全部经网关 8080 转发。路由配置在
`services/library-gateway/src/main/resources/application.yaml`：

| 路由 id | 断言 | 目标 |
| --- | --- | --- |
| library-user | `Path=/user/**,/admin/**,/reader/**` | `lb://library-user`（8081） |
| library-book | `Path=/book/**` | `lb://library-book`（8082，2 副本） |
| library-borrow | `Path=/borrow/**` | `lb://library-borrow`（8083） |

Spring Cloud Gateway 5.x 的配置层级为
`spring.cloud.gateway.server.webflux.routes`，与 4.x 的 `spring.cloud.gateway.routes`
不同，升级时需要注意。

### 1.2 服务发现与负载均衡

路由目标使用 `lb://服务名`，由 Nacos（group = `PERSONAL_GROUP`）提供实例列表，
Spring Cloud LoadBalancer 负责轮询。`library-book` 部署 2 个副本，因此 `/book/**`
的请求会在两个实例之间分摊，无需额外部署 Nginx。

### 1.3 全局 JWT 鉴权

`AuthGlobalFilter` 实现 `GlobalFilter`，对每个请求做统一校验：

1. 白名单直接放行：`/admin/login`、`/reader/login`、`/reader/loginout`、
   `/admin/loginout`、`/reader/register`、`/actuator/**`；
2. 其余请求读取 `Authorization` 头中的 token，用 `JwtTool` 解析；
3. 解析成功后把 `userId` 写入 `user-info` 请求头传给下游，下游服务从该头取当前用户；
4. 解析失败或缺少 token 时直接返回 `401`，请求不会到达业务服务。

这样各业务服务不需要重复实现登录校验，只信任网关注入的 `user-info`。

### 1.4 健康检查

四个服务统一使用 Spring Boot Actuator。网关在 `pom.xml` 中补充了
`spring-boot-starter-actuator`，并配置：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

`/actuator/health` 已在白名单内，可直接访问。

---

## 2. 运行命令

```bash
cd D:\microservices-practice-2412190216\services
docker compose up -d --build
docker compose ps
```

启动顺序由 `depends_on` 保证：MySQL/Nacos → user/book → borrow → gateway。
本机 3307、8848、18080 被其他容器占用，`services/.env` 中已将宿主机端口后移为
3308（MySQL）、8849（Nacos HTTP）、18081（Nacos 控制台）、9850/9851（gRPC）。

单独重建网关：

```bash
docker compose up -d --build gateway
```

---

## 3. 运行结果

### 3.1 容器状态

```text
library-monolith-gateway    Up (healthy)   0.0.0.0:8080->8080/tcp
library-monolith-user       Up (healthy)   0.0.0.0:8081->8081/tcp
library-monolith-borrow     Up (healthy)   0.0.0.0:8083->8083/tcp
zjgsu-library-monolith-book-service-1   Up (healthy)   8082/tcp
zjgsu-library-monolith-book-service-2   Up (healthy)   8082/tcp
library-monolith-mysql      Up (healthy)   0.0.0.0:3308->3306/tcp
library-monolith-nacos      Up             0.0.0.0:8849->8848/tcp
```

### 3.2 健康检查

```powershell
curl.exe http://localhost:8080/actuator/health
```

```json
{"status":"UP","groups":["liveness","readiness"]}
```

### 3.3 鉴权失败（未携带 token）

```powershell
curl.exe -i http://localhost:8080/book/admin/page
```

```text
HTTP/1.1 401 Unauthorized
```

请求被网关拦截，未转发到 book-service。

### 3.4 登录并访问受保护接口

```powershell
$token = (Invoke-RestMethod -Uri http://localhost:8080/admin/login -Method POST `
  -ContentType 'application/json' -Body '{"username":"admin","password":"123456"}').data
curl.exe -i http://localhost:8080/book/admin/page -H "Authorization: $token"
```

```text
HTTP/1.1 200
{"code":1,"data":{"total":5,"records":[...]},"msg":"success"}
```

### 3.5 服务注册

Nacos 控制台（`http://localhost:18081`，用户名 `nacos`）中可见：

| 服务名 | 实例数 |
| --- | --- |
| library-gateway | 1 |
| library-user | 1 |
| library-book | 2 |
| library-borrow | 1 |

---

## 4. 网关鉴权

### 4.1 实现思路

鉴权集中在网关的 `AuthGlobalFilter`（实现 `GlobalFilter`，`order = 0`），业务服务不再解析 token。
每个请求依次经过五步：

1. **白名单匹配**：`AntPathMatcher` 匹配 `AuthProperties.excludePaths`，命中直接放行，
   不校验 token；
2. **验签**：取 `Authorization` 头中的 token，`JwtTool.parseToken` 验签并校验 `exp`，
   缺失、签名错误或已过期统一返回 `401`；
3. **解析身份**：从 claims 取 `sub`（用户 id）与 `role`（`ADMIN` / `READER`）；
4. **角色校验**：路径含 `admin` 且角色不是 `ADMIN` → `403`；
   路径含 `reader` 且角色不是 `READER` → `403`；
5. **身份透传**：校验通过后把用户 id 写入 `user-info` 请求头，下游拦截器据此写入
   `BaseContext`。

几个设计取舍：

- token 直接放在 `Authorization` 头，不加 `Bearer ` 前缀，与前端约定一致；
- 网关不查数据库，角色完全来自 token 的 `role` claim，避免每次请求多一次查询；
- 401 表示“没登录/登录失效”，403 表示“登录了但角色不对”，便于前端区分处理；
- 白名单里的登录、注册接口不参与角色判断，否则会出现“登录也需要 token”的死循环。

### 4.2 运行命令

容器模式（推荐）：

```bash
cd D:\microservices-practice-2412190216\services
docker compose up -d --build gateway
docker compose logs -f gateway
```

本地调试：

```powershell
cd D:\microservices-practice-2412190216\services\library-gateway
mvn spring-boot:run
```

准备两个 token 用于验证：

```powershell
# 管理员 token
$at = (Invoke-RestMethod -Uri http://localhost:8080/admin/login -Method POST `
  -ContentType 'application/json' -Body '{"username":"admin","password":"123456"}').data

# 读者 token（读者账号需先注册或由管理员创建）
$rt = (Invoke-RestMethod -Uri http://localhost:8080/reader/login -Method POST `
  -ContentType 'application/json' -Body '{"username":"test_reader01","password":"123456"}').data
```

### 4.3 运行结果

| 场景 | 请求 | 结果 |
| --- | --- | --- |
| 未携带 token | `POST /user/admin/page` | `401 Unauthorized` |
| 读者 token 调管理端 | `POST /user/admin/page` + reader token | `403 Forbidden` |
| 管理员 token 调管理端 | `POST /user/admin/page` + admin token | `200`，返回用户分页 |
| 读者 token 调读者端 | `POST /book/reader/page` + reader token | `200`，返回图书分页 |
| 健康检查 | `GET /actuator/health` | `200`，`{"status":"UP"}` |

实际输出示例：

```text
# 无 token
HTTP/1.1 401 Unauthorized

# 读者 token 调管理端
HTTP/1.1 403 Forbidden

# 管理员 token 调管理端
HTTP/1.1 200
{"code":1,"data":{"total":5,"records":[...]},"msg":"success"}
```

结论：未登录与角色越权都在网关被拦下，请求不会到达下游服务；白名单接口与健康检查正常放行。

---

## 5. 认证服务（library-user）

### 5.1 实现思路

认证由 `library-user`（8081）承担，负责账号存储、登录校验、JWT 签发与读者注册，
不参与图书、借阅等业务数据。

#### 5.1.1 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/admin/login` | 管理员登录，校验 `account_type = ADMIN` |
| POST | `/reader/login` | 读者登录，校验 `account_type = READER` |
| POST | `/reader/register` | 读者注册，两次密码需一致 |
| GET | `/admin/loginout` | 管理员登出，返回成功 |
| GET | `/reader/loginout` | 读者登出，返回成功 |

登录请求体（`UserLoginDTO`）：

```json
{"username": "admin", "password": "123456"}
```

#### 5.1.2 登录校验顺序

`UserServiceImpl.adminLogin` / `readerLogin` 按固定顺序校验，任一步失败即抛
`BaseException`，由 `GlobalExceptionHandler` 统一转成 `code = 0` 的响应：

1. 按用户名查 `reader` 表，查不到 → “用户名错误”
2. 入参密码做 MD5：`password = DigestUtils.md5DigestAsHex(password.getBytes())`
3. 与库中 `reader.password` 比对，不一致 → “密码错误”
4. 账号类型必须与入口匹配，否则 → “该账号不是管理员/读者”
5. `status = 0`（停用）→ “账户已被停用，请联系管理员”

因此数据库里存的是密码的 MD5 值（32 位小写十六进制），而不是明文；
现有明文数据需要执行一次 `UPDATE library_user.reader SET password = MD5(password);`
迁移，且只能执行一次，否则会二次哈希。

#### 5.1.3 JWT 签发

`JwtUtil.createToken(userId, role)` 使用 `zyw.jwt.secret` 作为 HMAC 密钥：

- `sub`：用户 id
- `role`：`ADMIN` / `READER`
- `iat` / `exp`：签发时间与过期时间，有效期取 `zyw.jwt.expire-minutes`（120 分钟）

登录成功后接口返回：

```json
{"code": 1, "data": "<token>", "msg": "success"}
```

#### 5.1.4 下游取当前用户

网关 `AuthGlobalFilter` 验签成功后把用户 id 写入 `user-info` 请求头；
`JwtTokenAdminInterceptor` / `JwtTokenReaderInterceptor` 读取该头并写入
`BaseContext`（ThreadLocal），业务代码用 `BaseContext.getCurrentId()` 获取当前用户，
`afterCompletion` 中清理，避免线程复用串号。

服务自身不重复解析 token，只信任网关注入的头，这也是网关与认证服务的分工边界。

#### 5.1.5 注册

`POST /reader/register` 请求体为 `name`、`password`、`confirmPassword`：

- 两次密码不一致 → “密码不相同”
- 密码需满足 `^(?=.*[A-Za-z])(?=.*\d)\S{6,20}$` → 6~20 位且同时包含字母和数字
- 用户名重复 → “该用户名已重复”

注册与后台新增用户都应在入库前对密码做同样的 MD5，保证与登录校验方式一致。

---

### 5.2 运行命令

```bash
cd D:\microservices-practice-2412190216\services
docker compose up -d --build user-service
docker compose logs -f user-service
```

或本地直接运行 `LibraryUserApplication`（8081），依赖 MySQL 3307 与 Nacos 8848。

登录：

```powershell
curl.exe -i -X POST http://localhost:8080/admin/login `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"123456"}'
```

注册：

```powershell
curl.exe -i -X POST http://localhost:8080/reader/register `
  -H "Content-Type: application/json" `
  -d '{"name":"reader01","password":"abc123","confirmPassword":"abc123"}'
```

### 5.3 运行结果

管理员登录成功：

```text
HTTP/1.1 200
{"code":1,"data":"eyJhbGciOiJIUzI1NiJ9...","msg":"success"}
```

密码错误：

```text
{"code":0,"data":null,"msg":"密码错误"}
```

账号类型不匹配（用管理员账号走读者入口）：

```text
{"code":0,"data":null,"msg":"该账号不是读者"}
```

注册成功：

```text
{"code":1,"data":null,"msg":"success"}
```

数据库校验：

```sql
SELECT id, name, password, account_type, status FROM library_user.reader;
```

`password` 为 32 位 MD5 值，`account_type` 为 `ADMIN` / `READER`，`status = 1` 表示正常。

---

## 6. 测试脚本

### 6.1 实现思路

脚本位于 `scripts/auth-test.sh`，用 bash + curl 实现，不依赖 jq、Python 等额外工具，共 5 个断言：

| 序号 | 场景 | 请求 | 期望 |
| --- | --- | --- | --- |
| 1 | 未登录访问受保护接口 | `POST /book/reader/page`（无 token） | 401 |
| 2 | 登录获取 token | `POST /reader/login` | 200，`data` 非空 |
| 3 | 携带 token 访问受保护接口 | `POST /book/reader/page` + 读者 token | 200 且 `code=1` |
| 4 | 低权限 token 访问管理接口 | `POST /user/admin/page` + 读者 token | 403 |
| 5 | 对照组 | `POST /user/admin/page` + 管理员 token | 200 且 `code=1` |

实现要点：

- **状态码与响应体分离**：`curl -s -o "$BODY_FILE" -w '%{http_code}'`，响应体落临时文件，状态码直接用于断言；
- **JSON 取值**：`sed` 匹配 `"data"` / `"code"` 字段，兼容字符串与数字，避免依赖 jq；
- **结果统计**：`pass` / `fail` 函数累加 `PASS`、`FAIL`，最后输出汇总，`FAIL>0` 时退出码为 1，便于流水线判断；
- **可配置**：`BASE`、`READER_USER`、`READER_PASS`、`ADMIN_USER`、`ADMIN_PASS` 均可用环境变量覆盖；
- **清理**：`mktemp` 创建临时文件，`trap ... EXIT` 退出时删除。

### 6.2 运行命令

Git Bash：

```bash
cd /d/microservices-practice-2412190216
bash scripts/auth-test.sh
```

PowerShell 调用 Git Bash：

```powershell
cd D:\microservices-practice-2412190216
& 'C:\Program Files\Git\bin\bash.exe' scripts/auth-test.sh
```

前置条件：gateway（8080）、user-service（8081）、book-service（8082）已启动。

需要换账号时：

```bash
READER_USER=test_reader01 READER_PASS=123456 bash scripts/auth-test.sh
```

### 6.3 运行结果

```text
BASE=http://localhost:8080
READER=test_reader01  ADMIN=admin

==> 1. 未登录访问受保护接口，应失败
  [PASS] 无 Token 访问 /book/reader/page 返回 401

==> 2. 读者登录获取 Token
  [PASS] 读者登录成功，Token 长度 147

==> 3. 携带 Token 访问受保护接口，应成功
  [PASS] 读者 Token 访问 /book/reader/page 返回 200 且 code=1

==> 4. 低权限 Token 访问管理接口，应失败
  [PASS] 读者 Token 访问 /user/admin/page 返回 403，越权被拒

==> 5. 对照组：管理员 Token 访问管理接口，应成功
  [PASS] 管理员 Token 访问 /user/admin/page 返回 200 且 code=1

PASS=5 FAIL=0
```

结果说明：

- 无 token 被网关拦在 401，请求不会到达下游，证明“统一鉴权”生效；
- 读者 token 访问读者接口正常，说明验签与转发链路无问题；
- 读者 token 访问管理接口返回 403，证明“按角色分离”生效，越权无法穿透；
- 管理员 token 访问管理接口正常，排除“403 是因为接口本身不可用”的误判；
- 全部通过时脚本退出码为 0。

---

## 7. 小结

网关把路由、服务发现、鉴权、健康检查四处逻辑集中到一处：业务服务只关心自身接口，
新增服务只需在 `application.yaml` 增加一条 `lb://` 路由；`book-service` 扩容副本后
网关自动感知，无需改配置。鉴权统一在网关完成后，下游服务通过 `user-info` 头识别用户，
避免了每个服务各自解析 token 的重复代码。

认证服务与网关的边界是：网关只验签、不透传 token，认证服务只签发 token、不参与路由。
密码统一用 MD5 存储与校验，注册、新增用户、重置密码三处必须保持同一算法，
否则会出现“账号存在但密码永远错误”的问题。
