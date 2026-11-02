# Week 07 · Nacos 部署、初始化与服务注册

| 项         | 内容                                                         |
| ---------- | ------------------------------------------------------------ |
| 周次       | 第 07 周                                                     |
| 主题       | Nacos 注册中心部署与服务注册                                 |
| 运行环境   | Windows 11 + Docker Desktop + JDK 25 + Maven 3.9             |
| 服务端版本 | `nacos/nacos-server:v3.2.4`（standalone）                    |
| 客户端版本 | `nacos-client:3.1.1`（由 Spring Cloud Alibaba BOM 统一管理，未单独覆盖） |
| 完成时间   | 2026-11-02                                                   |

---

## 1. 端口规划

| 用途                  | 宿主机  | 容器内 | 说明                                   |
| --------------------- | ------- | ------ | -------------------------------------- |
| Nacos 控制台          | `18080` | `8080` | 浏览器访问，Nacos 3 起控制台为独立端口 |
| Nacos 服务端 HTTP     | `8848`  | `8848` | 注册、心跳、服务查询                   |
| Nacos gRPC 客户端     | `9848`  | `9848` | 必须映射，否则客户端长连接失败         |
| Nacos gRPC 服务端互联 | `9849`  | `9849` | 服务端之间通信                         |
| library-gateway       | `8080`  | —      | 网关                                   |
| library-user          | `8081`  | —      | 用户/鉴权服务                          |
| library-book          | `8082`  | —      | 图书服务                               |
| library-borrow        | `8083`  | —      | 借阅服务                               |

业务服务运行在宿主机，Nacos 运行在容器中，因此 `server-addr` 统一写 `127.0.0.1:8848`。
客户端会自动按 `8848 + 1000` 推算 gRPC 端口 `9848`，故 `9848` 必须映射到宿主机。

---

## 2. 依赖管理（统一 BOM）

根 `pom.xml` 只导入 BOM，不写 starter 版本：

```xml
<properties>
    <spring-cloud.version>2025.1.3</spring-cloud.version>
    <spring-cloud-alibaba.version>2025.1.0.0</spring-cloud-alibaba.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

各业务模块只声明 starter，不写 version，本周未引入 Config Starter：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

`mvn -pl library-book dependency:tree -Dincludes=com.alibaba.nacos` 实际输出：

```text
com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:jar:2025.1.0.0
\- com.alibaba.nacos:nacos-client:jar:3.1.1
```

---

## 3. Nacos 部署

### 3.1 compose.yaml

```yaml
name: zjgsu-library

services:
  nacos:
    image: nacos/nacos-server:v3.2.4
    container_name: nacos-standalone
    restart: unless-stopped
    environment:
      MODE: standalone
      PREFER_HOST_MODE: hostname
      NACOS_AUTH_ENABLE: "true"
      NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN}
      NACOS_AUTH_IDENTITY_KEY: ${NACOS_AUTH_IDENTITY_KEY}
      NACOS_AUTH_IDENTITY_VALUE: ${NACOS_AUTH_IDENTITY_VALUE}
      NACOS_AUTH_TOKEN_EXPIRE_SECONDS: "18000"
      JVM_XMS: 512m
      JVM_XMX: 512m
      JVM_XMN: 256m
    ports:
      - "${NACOS_CONSOLE_PORT}:8080"
      - "${NACOS_SERVER_PORT}:8848"
      - "${NACOS_GRPC_PORT}:9848"
      - "${NACOS_GRPC_SERVER_PORT}:9849"
    volumes:
      - ./nacos/data:/home/nacos/data
      - ./nacos/logs:/home/nacos/logs
```

### 3.2 凭据管理

- 真实 Token、Identity、管理员密码只写在本地 `.env`，`.env` 已在 `.gitignore` 中
- 仓库只提交 `.env.example` 占位模板
- `NACOS_AUTH_TOKEN` 为 base64 编码，解码后长度 ≥ 32 字节

```dotenv
# .env.example
NACOS_AUTH_TOKEN=<base64-encoded-secret-32-bytes-min>
NACOS_AUTH_IDENTITY_KEY=serverIdentity
NACOS_AUTH_IDENTITY_VALUE=<random-identity-value>
NACOS_AUTH_TOKEN_EXPIRE_SECONDS=18000
NACOS_USERNAME=nacos
NACOS_PASSWORD=<your-admin-password>
NACOS_CONSOLE_PORT=18080
NACOS_SERVER_PORT=8848
NACOS_GRPC_PORT=9848
NACOS_GRPC_SERVER_PORT=9849
```

### 3.3 启动

```powershell
cd D:\A\monolith
docker compose up -d
docker compose ps
docker compose logs -f nacos        # 等待 Nacos started successfully
```

数据与日志落在 `./nacos/data`、`./nacos/logs`，已被 git 忽略，容器重建后注册信息不丢。

---

## 4. 初始化

1. 浏览器打开 <http://127.0.0.1:18080>
2. 首次访问进入管理员初始化页，创建管理员账号（用户名 `nacos`，密码保存在本地 `.env`，未提交仓库）
3. 初始化完成后使用该账号登录控制台
4. `NACOS_AUTH_ENABLE=true` 已开启，未携带凭据的客户端注册会被拒绝

Nacos 3 不再提供默认 `nacos/nacos` 账号，控制台也不在 `8848/nacos`，
访问 8848 的 `/nacos` 返回 404 属正常现象。

---

## 5. 服务注册配置

四个模块结构一致，仅 `spring.application.name` 与 `server.port` 不同。

```yaml
server:
  port: 8082                       # user 8081 / borrow 8083 / gateway 8080

spring:
  application:
    name: library-book             # library-user / library-borrow / library-gateway
  cloud:
    nacos:
      username: nacos              # 账号密码位于 nacos 通用层
      password: ${NACOS_PASSWORD:nacos}
      discovery:
        server-addr: 127.0.0.1:8848
        ip: 127.0.0.1              # 固定注册 IP，避免多网卡注册成 172.x
```

要点：

- 各服务使用自己的 `spring.application.name`
- 宿主机端口 8080 / 8081 / 8082 / 8083 互不冲突
- 未引入 `spring-cloud-starter-alibaba-nacos-config`，未使用 `spring.config.import=nacos:`

---

## 6. 启动顺序与验证

```powershell
mvn -DskipTests clean install

mvn -pl library-book     spring-boot:run     # 8082
mvn -pl library-user     spring-boot:run     # 8081
mvn -pl library-borrow   spring-boot:run     # 8083
mvn -pl library-gateway  spring-boot:run     # 8080
```

注册成功的两个判据：

1. 服务日志出现：

```text
nacos registry, DEFAULT_GROUP library-book 127.0.0.1:8082 register finished
```

2. 控制台「服务管理 → 服务列表」可见该服务且健康实例数为 1。

---

## 7. 注册结果

控制台：<http://127.0.0.1:18080>（账号 `nacos`）

| 服务名          | 分组          | 实例地址       | 健康实例数 | 状态 |
| --------------- | ------------- | -------------- | ---------- | ---- |
| library-user    | DEFAULT_GROUP | 127.0.0.1:8081 | 1          | 健康 |
| library-book    | DEFAULT_GROUP | 127.0.0.1:8082 | 1          | 健康 |
| library-borrow  | DEFAULT_GROUP | 127.0.0.1:8083 | 1          | 健康 |
| library-gateway | DEFAULT_GROUP | 127.0.0.1:8080 | 1          | 健康 |

3 个业务服务 + 1 个网关，全部注册在 `DEFAULT_GROUP`，实例 IP 均为 `127.0.0.1`。

---

## 8. 截图

| 编号 | 文件                                 | 内容                                                     |
| ---- | ------------------------------------ | -------------------------------------------------------- |
| 01   | `screenshots/01-nacos-services.png`  | 控制台服务列表：4 个服务名、分组、实例数                 |
| 02   | `screenshots/02-instance-detail.png` | 服务详情：实例 IP 与端口、健康状态                       |
| 03   | `screenshots/03-compose-ps.png`      | `docker compose ps`：nacos 容器 Up，端口 18080/8848/9848 |
| 04   | `screenshots/04-register-log.png`    | 服务启动日志中的 `register finished`                     |

![服务列表](screenshots/01-nacos-services.png)
![实例详情](screenshots/02-instance-detail.png)
![容器状态](screenshots/03-compose-ps.png)
![注册日志](screenshots/04-register-log.png)

> 截图不包含 `.env`、Token、管理员密码，Nacos 未映射到公网。

---

## 9. 遇到的问题与处理

| 现象                                                 | 原因                                                         | 处理                                                     |
| ---------------------------------------------------- | ------------------------------------------------------------ | -------------------------------------------------------- |
| 日志反复 `Client not connected`，实例注册不上        | 只映射了 8848，未映射 gRPC 端口 9848                         | compose 增加 `9848:9848`、`9849:9849`                    |
| 注册返回 `403 / user not found`                      | 开启鉴权但客户端未携带凭据                                   | 配置 `spring.cloud.nacos.username/password`              |
| YAML 中 `spring.cloud.nacos.discovery.username` 标红 | 该版本凭据属性位于 `spring.cloud.nacos` 层，不在 `discovery` 下 | 账号密码上移一层，`discovery` 只保留 `server-addr`、`ip` |
| 网关访问 `/borrow/**` 返回 404                       | 路由谓词误写成 `Path=/book/**`，与图书服务重复               | 改为 `Path=/borrow/**` 后重启网关                        |
| 图书分页接口 `Invalid bound statement`               | `mapper-locations` 被写在 `mybatis.configuration` 下，未生效 | 上移到 `mybatis.mapper-locations` 并重新构建             |

---

## 10. 复现步骤

```powershell
# 1. 准备凭据
Copy-Item .env.example .env      # 填入 Token / Identity / 管理员密码

# 2. 启动 Nacos
docker compose up -d
# 浏览器打开 http://127.0.0.1:18080 完成管理员初始化

# 3. 构建所有模块
mvn -DskipTests clean install

# 4. 依次启动服务
mvn -pl library-book     spring-boot:run
mvn -pl library-user     spring-boot:run
mvn -pl library-borrow   spring-boot:run
mvn -pl library-gateway  spring-boot:run

# 5. 控制台确认 4 个服务均为健康状态
```

## 11. 跨服务调用（借书前校验图书）

### 11.1 调用方向

| 项         | 内容                                                         |
| ---------- | ------------------------------------------------------------ |
| 调用方     | library-borrow（8083）                                       |
| 被调用方   | library-book（8082）                                         |
| 实际服务名 | `library-book`                                               |
| 注册分组   | `PERSONAL_GROUP`                                             |
| 命名空间   | public（默认）                                               |
| 调用方式   | 声明式服务发现 `@FeignClient("library-book")`，URL 中不出现 IP 与端口，由 Nacos 解析实例地址 |
| 超时配置   | 连接 2000ms / 读取 3000ms                                    |
| 业务位置   | `BorrowServiceImpl#CheckRecord`（借书前校验）、`CreateRecord`（回写图书状态） |

调用链路：

```text
前端/网关 -> library-borrow(8083) /borrow/admin
          -> Feign(@FeignClient "library-book")
          -> Nacos 解析 PERSONAL_GROUP 下实例 127.0.0.1:8082
          -> library-book  GET /book/reader/{id}
          -> 校验通过：POST /book/admin/update 改状态 -> 写 borrow_record
```

### 11.2 请求示例

借阅成功场景（图书存在且在馆）：

```http
POST http://127.0.0.1:8083/borrow/admin
Authorization: Bearer <读者/管理员 token>
Content-Type: application/json

{
  "bookId": 1,
  "readerId": 2,
  "dueDate": "2026-12-02"
}
```

PowerShell 等价命令：

```powershell
curl -X POST http://127.0.0.1:8083/borrow/admin `
  -H "Authorization: Bearer <token>" `
  -H "Content-Type: application/json" `
  -d "{\"bookId\":1,\"readerId\":2,\"dueDate\":\"2026-12-02\"}"
```

校验失败场景（图书不存在）：把 `bookId` 换成数据库中不存在的值，例如 `999999`。

### 11.3 预期结果

| 场景               | HTTP 状态 | 响应体                            | 数据落库           | 图书状态        |
| ------------------ | --------- | --------------------------------- | ------------------ | --------------- |
| 图书存在且在馆     | 200       | `{"code":1,"msg":"success"}`      | 新增 borrow_record | 改为 `BORROWED` |
| 图书不存在         | 200       | `{"code":0,"msg":"图书不存在"}`   | 不新增             | 不变            |
| 图书已借出         | 200       | `{"code":0,"msg":"图书无库存"}`   | 不新增             | 不变            |
| 读者借阅超上限     | 200       | `{"code":0,"msg":"超出借阅上限"}` | 不新增             | 不变            |
| 图书服务无可用实例 | 500       | 异常堆栈                          | 不新增             | 不变            |

> 说明：本项目沿用统一返回体 `{code,msg,data}`，业务失败返回 HTTP 200 + `code=0`；
> 校验不通过时在 `@Transactional` 内抛出 `BaseException`，事务回滚，因此不会产生脏数据。

### 11.4 实际结果

| 场景                     | 实际响应                              | 数据库核对                                                   |
| ------------------------ | ------------------------------------- | ------------------------------------------------------------ |
| 借书 `bookId=1`          | 200，`{"code":1,"msg":"success"}`     | `borrow_record` 新增 1 条，`status=BORROWED`；`book.id=1` 状态变为 `BORROWED` |
| 借书 `bookId=999999`     | 200，`{"code":0,"msg":"图书不存在"}`  | `borrow_record` 无新增                                       |
| 重复借已借出的图书       | 200，`{"code":0,"msg":"图书无库存"}`  | `borrow_record` 无新增                                       |
| 停掉 library-book 后再借 | 500，日志提示 LoadBalancer 无可用实例 | `borrow_record` 无新增                                       |

Nacos 控制台核对：`PERSONAL_GROUP` 分组下 `library-book` 健康实例 1 个，地址 `127.0.0.1:8082`。

## 12. 依赖服务下线与恢复验证

调用链路：library-borrow → Feign(`library-book`) → Nacos `PERSONAL_GROUP` → `127.0.0.1:8082`

### 12.1 停止与恢复

```powershell
# 停止
Get-NetTCPConnection -LocalPort 8082 -State Listen | Select-Object -ExpandProperty OwningProcess
Stop-Process -Id <pid> -Force

# 恢复
mvn -pl library-book spring-boot:run
```

### 12.2 业务请求

```http
POST http://127.0.0.1:8083/borrow/admin
Authorization: Bearer <token>
Content-Type: application/json

{"bookId":3,"readerId":2,"dueDate":"2026-12-03"}
```

### 12.3 状态码与数据检查

| 阶段       | HTTP | 响应                                         | Nacos 实例                 | 数据检查                   |
| ---------- | ---- | -------------------------------------------- | -------------------------- | -------------------------- |
| 正常       | 200  | `code=1`                                     | 1 个，健康                 | 记录 +1，图书转 `BORROWED` |
| 停止后     | 500  | `Load balancer does not contain an instance` | 约 15s 不健康、约 30s 摘除 | 无新增，图书状态不变       |
| 恢复后重试 | 200  | `code=1`                                     | 重新注册为 1 个            | 记录 +1，状态正确          |

数据核对 SQL：

```sql
SELECT COUNT(*) FROM borrow_record WHERE book_id = 3;   -- 失败后 0，恢复后 1
SELECT id, status FROM book WHERE id = 3;               -- 失败后 AVAILABLE，恢复后 BORROWED
```

校验失败时在写入前抛异常，`@Transactional` 回滚，未产生本地记录。

### 12.4 健康状态区别

| 概念              | 含义                                                         |
| ----------------- | ------------------------------------------------------------ |
| SDK 连接存活      | 客户端与注册中心（8848/9848）通信正常，不代表业务可用        |
| 注册实例状态      | 由心跳维护，进程停止后自动不健康并摘除                       |
| Actuator 应用健康 | 应用内部组件（DB 等）状态，默认不上报 Nacos，`DOWN` 不会自动摘除实例 |

服务发现只解决寻址，不消除跨服务写操作的部分成功窗口。

## 13. 冒烟脚本、测试与整体结论

### 13.1 脚本检查项

| 检查            | 地址                                    | 通过条件     |
| --------------- | --------------------------------------- | ------------ |
| Nacos 控制台    | `http://127.0.0.1:18080/`               | HTTP 200/302 |
| library-gateway | `http://127.0.0.1:8080/actuator/health` | `status: UP` |
| library-user    | `http://127.0.0.1:8081/actuator/health` | `status: UP` |
| library-book    | `http://127.0.0.1:8082/actuator/health` | `status: UP` |
| library-borrow  | `http://127.0.0.1:8083/actuator/health` | `status: UP` |

控制台使用 Nacos 3 的 8080 端口（宿主机映射 18080），脚本显式拒绝 `8848/nacos` 旧地址；本地检查统一加 `--noproxy '*'`。

### 13.2 执行命令

```powershell
# 仓库根目录
& "C:\Program Files\Git\bin\bash.exe" scripts/smoke-test.sh

# 已在 scripts 目录内
& "C:\Program Files\Git\bin\bash.exe" ./smoke-test.sh

# 查看退出码
$LASTEXITCODE
```

脚本需在 Windows 的 Git Bash 中运行；WSL 中 `127.0.0.1` 指向 Linux 虚拟机，需改用宿主机 IP（`SMOKE_HOST` 可覆盖）。

### 13.3 退出结果

| 场景                | 输出                 | 退出码 |
| ------------------- | -------------------- | ------ |
| 全部正常            | 5 个 PASS            | 0      |
| 停止 library-book   | 1 个 FAIL，其余 PASS | 1      |
| 控制台地址误配 8848 | 地址错误提示         | 2      |

任一检查失败均以非零退出，不会忽略错误后报告成功。

### 13.4 测试

```bash
mvn -pl library-borrow test
```

- 使用 `test` profile：`spring.cloud.nacos.discovery.enabled=false`、`register-enabled=false`，不连真实 Nacos
- 数据源为 H2 内存库（`jdbc:h2:mem:borrow;MODE=MySQL`）
- 远程调用通过 Mock 模拟：验证"远程图书不存在时不写借阅记录"（`verify(borrowMapper, never()).createRecord(...)`）

### 13.5 整体验证结论

| 验证项             | 结果                                                         |
| ------------------ | ------------------------------------------------------------ |
| Nacos 部署与初始化 | 控制台 18080 可访问，管理员账号已初始化                      |
| 服务注册           | PERSONAL_GROUP 下 4 个服务各 1 个健康实例（127.0.0.1:8080-8083） |
| 跨服务调用         | 借书成功返回 `code=1` 并落库；图书不存在返回业务错误且不落库 |
| 下线与恢复         | 停止 library-book 后实例约 30s 被摘除，调用失败且无脏数据；重启后恢复成功 |
| 冒烟脚本           | 服务正常时 5 项全 PASS、退出码 0；停服务后退出码 1           |
| 测试               | H2 + Mock 远程，不依赖真实 Nacos 与 MySQL                    |

结论：控制台与健康端点可达只说明基础设施在线，业务是否可用仍以业务请求结果为准；服务发现只解决寻址，不消除跨服务写操作的部分成功窗口。

