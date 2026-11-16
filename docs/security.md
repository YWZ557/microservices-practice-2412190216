# 安全设计：认证流程、角色权限与网关路由

本文档记录图书馆借阅系统的认证与授权设计，对应实现位于 `services/library-user`（认证）与
`services/library-gateway`（网关鉴权）。

---

## 1. 认证流程

### 1.1 角色与账号存储

账号统一存放在 `library_user.reader` 表：

| 字段 | 说明 |
| --- | --- |
| `name` | 登录名，唯一 |
| `password` | 密码的 MD5 值（32 位小写十六进制） |
| `account_type` | `ADMIN`（管理员）/ `READER`（读者） |
| `status` | `1` 正常 / `0` 停用 |

初始化账号：`admin / 123456`（ADMIN）；读者账号可通过 `/reader/register` 注册，
或先用管理员在后台创建。

### 1.2 登录与 token 签发

| 接口 | 说明 |
| --- | --- |
| `POST /admin/login` | 管理员登录，要求 `account_type = ADMIN` |
| `POST /reader/login` | 读者登录，要求 `account_type = READER` |
| `POST /reader/register` | 读者注册，两次密码需一致，密码 6~20 位且含字母和数字 |

请求体：

```json
{"username": "admin", "password": "123456"}
```

校验顺序（任一步失败抛 `BaseException`，全局异常返回 `code = 0`）：

1. 按用户名查 `reader` 表，不存在 → “用户名错误”；
2. 入参密码做 MD5 后与 `password` 比对，不一致 → “密码错误”；
3. 账号类型与入口不匹配 → “该账号不是管理员/读者”；
4. `status = 0` → “账户已被停用，请联系管理员”。

校验通过后由 `JwtUtil.createToken(userId, accountType)` 签发 JWT：

| 内容 | 值 |
| --- | --- |
| 算法 | HMAC-SHA256（密钥为 `zyw.jwt.secret`） |
| `sub` | 用户 id |
| `role` | `ADMIN` / `READER` |
| `iat` / `exp` | 签发时间 / 过期时间，有效期 `zyw.jwt.expire-minutes`（120 分钟） |

响应：

```json
{"code": 1, "data": "<token>", "msg": "success"}
```

### 1.3 携带 token 访问

客户端把 token **原值**放入 `Authorization` 头，不要加 `Bearer ` 前缀：

```http
GET /book/admin/page HTTP/1.1
Host: localhost:8080
Authorization: eyJhbGciOiJIUzI1NiJ9...
```

### 1.4 一次请求的完整链路

```text
客户端
  │  Authorization: <token>
  ▼
library-gateway (8080)
  │ 1. 白名单？→ 直接转发
  │ 2. 验签（JwtTool.parseToken）失败 → 401
  │ 3. 路径含 admin 且 role ≠ ADMIN → 403
  │    路径含 reader 且 role ≠ READER → 403
  │ 4. 写入 user-info: <userId> 后转发
  ▼
library-user / library-book / library-borrow
  │ JwtTokenAdminInterceptor / JwtTokenReaderInterceptor
  │ 读取 user-info → BaseContext.setCurrentId(...)
  ▼
Controller → Service，通过 BaseContext.getCurrentId() 获取当前用户
```

`afterCompletion` 中调用 `BaseContext.remove()` 清理 ThreadLocal，避免线程复用导致用户串号。

### 1.5 登出与失效

`GET /admin/loginout`、`GET /reader/loginout` 仅返回成功。JWT 无状态，服务端不保存会话，
登出由客户端丢弃 token 完成；token 在 `exp` 到期后自然失效，届时请求返回 `401`。

---

## 2. 角色权限

### 2.1 角色定义

| 角色 | 常量 | 权限范围 |
| --- | --- | --- |
| 管理员 | `ADMIN` | 用户、图书、借阅记录的全部管理端接口 |
| 读者 | `READER` | 仅读者端接口：查询图书、借阅/归还、查看本人记录 |

### 2.2 接口与角色映射

| 接口路径 | 允许角色 | 其他角色 | 校验位置 |
| --- | --- | --- | --- |
| `/user/admin/**` | ADMIN | 403 | 网关 `AuthGlobalFilter` |
| `/book/admin/**` | ADMIN | 403 | 网关 `AuthGlobalFilter` |
| `/borrow/admin/**` | ADMIN | 403 | 网关 `AuthGlobalFilter` |
| `/user/reader/**` | READER | 403 | 网关 `AuthGlobalFilter` |
| `/book/reader/**` | READER | 403 | 网关 `AuthGlobalFilter` |
| `/borrow/reader/**` | READER | 403 | 网关 `AuthGlobalFilter` |

判断方式：请求路径包含 `admin` 且 token 角色不是 `ADMIN` 返回 403；包含 `reader` 且角色不是
`READER` 返回 403。角色不匹配时不进入下游服务。

### 2.3 登录入口的角色隔离

即使 token 合法，登录入口也要求账号类型匹配：管理员账号走 `/reader/login` 会被拒绝（反之亦然），
从源头避免“用读者身份拿管理员 token”。

### 2.4 状态码约定

| 状态码 | 含义 | 触发场景 |
| --- | --- | --- |
| 401 | 未认证 | 缺少 `Authorization`、token 非法或已过期 |
| 403 | 已认证但无权限 | 角色与接口不匹配；账号类型与登录入口不匹配（业务层返回 `code = 0`） |
| 200 | 正常 | 鉴权通过并成功转发 |

---

## 3. 网关路由表

网关配置位于 `services/library-gateway/src/main/resources/application.yaml`，
层级为 Spring Cloud Gateway 5.x 的 `spring.cloud.gateway.server.webflux.routes`。

| 路由 id | 断言 | 目标服务 | 端口 |
| --- | --- | --- | --- |
| library-user | `Path=/user/**,/admin/**,/reader/**` | `lb://library-user` | 8081 |
| library-book | `Path=/book/**` | `lb://library-book` | 8082（2 副本） |
| library-borrow | `Path=/borrow/**` | `lb://library-borrow` | 8083 |

- `lb://` 表示通过 Nacos（group `PERSONAL_GROUP`）发现实例，由 Spring Cloud LoadBalancer 轮询；
- `library-book` 部署 2 个副本，`/book/**` 的请求在两个实例间分摊；
- 网关自身端口 8080，是外部唯一入口；`/actuator/health` 暴露健康检查。

### 3.1 鉴权白名单

```text
/actuator/**
/admin/login
/admin/loginout
/reader/login
/reader/loginout
/reader/register
```

白名单按 Ant 风格匹配（`AntPathMatcher`），命中后跳过 token 校验。

### 3.2 健康检查

| 服务 | 地址 |
| --- | --- |
| 网关 | `http://localhost:8080/actuator/health` |
| 用户服务 | `http://localhost:8081/actuator/health` |
| 图书服务 | `http://localhost:8082/actuator/health` |
| 借阅服务 | `http://localhost:8083/actuator/health` |

---

## 4. 已知限制与加固方向

当前实现已覆盖“未登录拦截”与“角色不匹配拦截”，以下三项尚未落地：

1. **伪造身份头**：网关只写入 `user-info`，未显式清除客户端传入的同名头，也未注入
   `X-User-Role` 供下游二次校验；
2. **下游二次校验**：`JwtTokenAdminInterceptor` / `JwtTokenReaderInterceptor` 只读取
   `user-info`，不校验角色；
3. **绕过网关直连**：`compose.yaml` 中 user-service（8081）、borrow-service（8083）仍映射
   宿主机端口，理论上可绕过网关直接访问。

加固方案：

- 网关在过滤链最前面 `remove("user-info")`、`remove("X-User-Role")`，验签通过后 `set` 注入；
- 下游拦截器校验 `X-User-Role` 与路径是否匹配，不匹配返回 403；
- `compose.yaml` 中下游服务只保留 `expose`，删除宿主机 `ports` 映射，仅网关对外暴露；
- 密码可进一步由 MD5 升级为 BCrypt 加盐存储。
