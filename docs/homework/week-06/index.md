# 第 06 周作业：微服务拆分

## 服务拆分设计

### 1. 实现思路

本次拆分以业务领域为边界，而不是按 Controller、Service、Repository 技术层拆分。原单体中的图书、读者、借阅三类能力分别迁移为三个独立 Spring Boot 服务，`monolith/` 保留作为单体版本。

拆分遵循以下原则：

- 每个服务有独立 `pom.xml`、启动类、端口和数据库；
- 每张表只归一个服务所有，跨服务只保存对方 ID；
- 不建立跨库外键，不跨库 JOIN，不调用对方 Mapper/Repository；
- 服务之间通过 HTTP 接口协作，读者姓名等信息通过 API 回填；
- 借阅服务负责跨服务流程编排，并显式处理超时和依赖失败。

完整设计、服务职责表和 Mermaid 架构图见：

```text
docs/microservices-design.md
```

### 2. 服务划分

| 服务 | 端口 | 独立数据库 | 拥有的表 | 职责 |
|---|---:|---|---|---|
| `user-service` | 8081 | `library_user` | `reader` | 管理员/读者登录、读者信息、账号状态、JWT 签发 |
| `book-service` | 8082 | `library_book` | `book` | 图书增删改查、借出/归还状态维护 |
| `borrow-service` | 8083 | `library_borrow` | `borrow_record` | 借书、还书、借阅记录、逾期检查、跨服务编排 |

服务间调用关系：

```text
borrow-service
  -> user-service   GET  /internal/readers/{id}
  -> user-service   GET  /internal/readers/search?name=xxx
  -> user-service   POST /internal/readers/batch
  -> book-service   GET  /internal/books/{id}
  -> book-service   POST /internal/books/{id}/borrow
  -> book-service   POST /internal/books/{id}/return
```

`user-service` 和 `book-service` 是叶子服务，不调用其他服务；`borrow-service` 是唯一的流程编排服务。

### 3. 运行命令

#### 3.1 初始化独立数据库

```powershell
mysql -uroot -p123456 < services/db/init.sql
```

脚本会创建：

- `library_user`、`library_book`、`library_borrow`；
- `reader`、`book`、`borrow_record` 三张表的数据副本；
- `user_svc`、`book_svc`、`borrow_svc` 三个独立账号。

#### 3.2 编译三个服务

```powershell
mvn -f services/user-service/pom.xml -DskipTests package
mvn -f services/book-service/pom.xml -DskipTests package
mvn -f services/borrow-service/pom.xml -DskipTests package
```

#### 3.3 分别启动

```powershell
mvn -f services/user-service/pom.xml spring-boot:run
mvn -f services/book-service/pom.xml spring-boot:run
mvn -f services/borrow-service/pom.xml spring-boot:run
```

#### 3.4 健康检查

```powershell
curl.exe http://localhost:8081/actuator/health
curl.exe http://localhost:8082/actuator/health
curl.exe http://localhost:8083/actuator/health
```

### 4. 验证结果

三个服务均编译成功：

```text
user-service    BUILD SUCCESS
book-service    BUILD SUCCESS
borrow-service  BUILD SUCCESS
```

三个健康检查均返回 HTTP 200：

```text
http://localhost:8081/actuator/health -> 200
http://localhost:8082/actuator/health -> 200
http://localhost:8083/actuator/health -> 200
```

借阅主流程验证：

| 场景 | 请求 | 预期 | 实际结果 |
|---|---|---|---|
| 读者登录 | `POST /api/reader/login` | 返回 token | `code=1`，成功获取 token |
| 借阅列表 | `POST /api/reader/borrow/page` | 返回借阅记录及读者姓名 | `code=1`，姓名由 `user-service` 回填 |
| 借书 | `POST /api/reader/borrow` | 图书变为 `BORROWED`，本地新增记录 | `code=1`，验证通过 |
| 还书 | `GET /api/reader/borrow/return/{id}` | 图书恢复 `AVAILABLE`，记录变为 `RETURNED` | `code=1`，验证通过 |

失败场景验证：

| 场景 | 预期 | 实际结果 |
|---|---|---|
| 图书不存在 `bookId=9999` | 返回明确错误，不写借阅记录 | `code=0`，记录数不变 |
| 读者不存在 `readerId=9999` | 返回明确错误，不改图书状态 | `code=0`，图书仍为 `AVAILABLE` |
| 停止 `book-service` 后借书 | 返回服务不可用，不写借阅记录 | `code=0`，提示“图书服务不可用，请稍后重试” |

验证结束后已清理测试借阅记录，数据库数据量恢复为（与 `services/db/init.sql` 种子数据一致）：

```text
library_user.reader           3 条
library_book.book             5 条
library_borrow.borrow_record  1 条
```

### 5. 本地编排的完成结果与验证方法

#### 5.1 编排内容

编排文件为项目根目录的 `compose.yaml`，一次性拉起 4 个容器，容器之间通过自定义网络 `library-net` 以服务名互相访问：

| 容器 | 镜像 | 端口映射 | 依赖 |
|---|---|---|---|
| `library-mysql` | `mysql:8.0` | `3307:3306` | — |
| `library-user-service` | `library/user-service:week06` | `8081:8081` | `mysql` 健康后启动 |
| `library-book-service` | `library/book-service:week06` | `8082:8082` | `mysql` 健康后启动 |
| `library-borrow-service` | `library/borrow-service:week06` | `8083:8083` | `mysql`、`user-service`、`book-service` 全部健康后启动 |

关键编排点：

1. 数据持久化：命名卷 `library-mysql-data` 挂载到 `/var/lib/mysql`，`docker compose down` 不删除卷，重建容器后数据保留。
2. 首次初始化：`./services/db/init.sql` 挂载到 `/docker-entrypoint-initdb.d/01-init.sql`，仅在数据卷为空时执行，创建三个独立数据库、三个独立账号及各自的表。
3. 数据库访问隔离：每个服务通过 `SPRING_DATASOURCE_*` 环境变量连接自己的库（`user_svc`/`book_svc`/`borrow_svc`），不使用 root 账号。
4. 服务地址注入：`borrow-service` 通过 `READER_SERVICE_URL=http://user-service:8081`、`BOOK_SERVICE_URL=http://book-service:8082` 调用叶子服务，容器内不使用 `localhost`。
5. 启动顺序与自愈：`depends_on` 使用 `condition: service_healthy`，并为每个服务配置 healthcheck，`restart: unless-stopped` 保证异常退出后自动拉起。

#### 5.2 完成结果

一键启动 `docker compose up -d --build` 后：

```text
NAME                     IMAGE                           STATUS                        PORTS
library-book-service     library/book-service:week06     Up (healthy)                  0.0.0.0:8082->8082/tcp
library-borrow-service   library/borrow-service:week06   Up (healthy)                  0.0.0.0:8083->8083/tcp
library-mysql            mysql:8.0                       Up (healthy)                  0.0.0.0:3307->3306/tcp
library-user-service     library/user-service:week06     Up (healthy)                  0.0.0.0:8081->8081/tcp
```

执行 `docker compose down`（保留卷）再 `docker compose up -d` 重建全部容器后，4 个容器再次全部为 `Up (healthy)`，三个服务健康检查均返回：

```text
http://localhost:8081/actuator/health -> {"status":"UP"}
http://localhost:8082/actuator/health -> {"status":"UP"}
http://localhost:8083/actuator/health -> {"status":"UP"}
```

三者 `components.db.status` 均为 `UP`，说明 MySQL 重建后三个服务的数据源连接池已正常恢复。

#### 5.3 验证方法

验证分四层，每层都有明确的通过判据：

| 层次 | 命令 | 通过判据 |
|---|---|---|
| 容器状态 | `docker compose ps` | 4 个容器均为 `Up (healthy)`，端口映射与设计一致 |
| 服务健康 | `curl.exe http://localhost:808X/actuator/health` | HTTP 200，`status=UP` 且 `components.db.status=UP` |
| 业务链路 | 登录 → 借书 → 还书（见第 4 节） | 接口 `code=1`，`book.status` 在 `AVAILABLE`/`BORROWED` 间正确切换，`borrow_record` 数量随之增减 |
| 数据持久化 | `docker compose down` → `docker compose up -d` → 查库 | 重启后行数与“数据指纹”均不变 |

持久化验证之所以可信，是因为不只看行数，还比对了种子行的 `create_time` 指纹：若数据卷被删除，`init.sql` 会重新执行并生成新的 `NOW()` 时间戳：

```powershell
# 重启前记录指纹
docker compose exec -T mysql mysql -uroot -proot123456 -N -e `
  "SELECT id,create_time FROM library_user.reader ORDER BY id;"

docker compose down          # 不删除命名卷
docker compose up -d

# 重启后再次比对
docker compose exec -T mysql mysql -uroot -proot123456 -N -e `
  "SELECT id,create_time FROM library_user.reader ORDER BY id;
   SELECT 'books',COUNT(*) FROM library_book.book;"
```

实际输出重启前后完全一致，说明数据来自原卷而非重新初始化：

```text
1  2026-10-26 19:58:28
2  2026-10-26 19:58:28
3  2026-10-26 19:58:28
books    5
borrows  1
```

另外单独执行 `docker compose restart mysql` 做单容器重启演练，等待约 15 秒后容器恢复 `(healthy)`，`library_book.book` 仍为 5 条，三个服务健康检查同步恢复为 `UP`，说明短暂断连后连接池可自愈。

验证结束后的收尾：删除本次测试产生的 `borrow_record` 记录，恢复 `readers=3 / books=5 / borrows=1` 的基线数据，并确认被借出的 `book.id=5` 已回到 `AVAILABLE`。

### 6. 关键结论

1. 按业务领域拆分后，三个服务职责清晰，读者、图书、借阅各自独占数据。
2. `borrow-service` 通过 `RestTemplate` 调用 `user-service` 和 `book-service`，不再直接访问对方表，也不再使用跨库 JOIN。
3. 借阅列表中的读者姓名通过读者服务批量接口回填，避免跨库查询。
4. 依赖服务不可用或资源不存在时，借阅服务会返回明确错误，并且不会继续写本地数据。
5. 当前仍存在“远端扣借成功、本地写记录失败”的一致性窗口，本周通过顺序校验和失败不写本地数据控制，后续可用补偿或消息机制完善。
6. `compose.yaml` 实现了一键本地编排：依赖健康检查控制启动顺序，服务名代替 IP 完成服务发现，命名卷保证容器重建后数据不丢，实测 `down` + `up` 后数据指纹与行数均未变化。

### 5. 本地编排

#### 5.1 实现思路

目标是一条命令拉起 MySQL 与三个微服务，并且启动顺序、网络寻址、数据持久化都不需要人工干预。编排文件为项目根目录的 `compose.yaml`，一次性创建 4 个容器，容器之间通过自定义网络 `library-net` 以服务名互相访问：

| 容器                     | 镜像                            | 端口映射    | 依赖                                                   |
| ------------------------ | ------------------------------- | ----------- | ------------------------------------------------------ |
| `library-mysql`          | `mysql:8.0`                     | `3307:3306` | —                                                      |
| `library-user-service`   | `library/user-service:week06`   | `8081:8081` | `mysql` 健康后启动                                     |
| `library-book-service`   | `library/book-service:week06`   | `8082:8082` | `mysql` 健康后启动                                     |
| `library-borrow-service` | `library/borrow-service:week06` | `8083:8083` | `mysql`、`user-service`、`book-service` 全部健康后启动 |

关键编排点：

1. 启动顺序：`depends_on` 使用 `condition: service_healthy`，`borrow-service` 必须等三个依赖健康后才启动，避免“先启动先报错”。
2. 服务发现：容器内一律使用服务名通信（`mysql:3306`、`http://user-service:8081`、`http://book-service:8082`），不使用 `localhost`。
3. 数据隔离：三个服务分别用 `user_svc`、`book_svc`、`borrow_svc` 账号连接各自的库，不使用 root。
4. 数据持久化：命名卷 `library-mysql-data` 挂载到 `/var/lib/mysql`，`down` 不删卷，重建容器后数据保留。
5. 首次初始化：`./services/db/init.sql` 挂载到 `/docker-entrypoint-initdb.d/01-init.sql`，仅在数据卷为空时执行，创建三个库、三个账号及各自的表。
6. 端口与自愈：端口通过 `${MYSQL_PORT:-3307}` 等变量可覆盖，避免与本机端口冲突；`restart: unless-stopped` 保证异常退出后自动拉起。

#### 5.2 运行命令

前置条件：Docker Desktop 已启动，8081/8082/8083/3307 未被占用。

```powershell
cd d:\microservices-practice-2412190216

# 1) 构建镜像并后台启动全部服务（首次或代码变更后执行）
docker compose up -d --build

# 2) 查看容器状态与端口映射
docker compose ps

# 3) 排查启动问题
docker compose logs -f borrow-service

# 4) 健康检查
curl.exe http://localhost:8081/actuator/health
curl.exe http://localhost:8082/actuator/health
curl.exe http://localhost:8083/actuator/health

# 5) 进入 MySQL 核对数据（宿主机端口 3307）
docker compose exec -T mysql mysql -uroot -proot123456 -e `
  "SELECT COUNT(*) FROM library_book.book;"

# 6) 停止容器（保留命名卷，数据不丢）
docker compose down

# 7) 彻底重置：删除容器与数据卷，下次启动会重新执行 init.sql
docker compose down -v

# 8) 只重启数据库，验证服务自愈
docker compose restart mysql
```

### 6. 验证

#### 6.1 实现思路

把验证固化成脚本 `scripts/smoke-test.sh`，用 HTTP 断言代替人工点页面，可反复执行：

1. 判定依据是响应体的 `code` 字段。`GlobalExceptionHandler` 会把业务异常统一包装成 `Result`，业务失败同样是 **HTTP 200**，只看状态码会误判为成功。
2. 数据核对走接口：图书状态查 `/api/reader/book/{id}`，借阅记录数与状态查 `/api/reader/borrow/page`；检测到 docker 可用时再用 SQL 交叉校验，不可用则标记 `[SKIP]`，不影响结论。
3. 脚本自动挑选一本 `AVAILABLE` 图书、自动获取新记录 id，结束时调用 `DELETE /api/reader/borrow/{id}` 清理，跑完数据回到基线。
4. 覆盖四组场景：健康检查、借书→还书主流程、依赖资源不存在、依赖服务不可用（`WITH_OUTAGE=1` 时临时停掉 `book-service`）。

#### 6.2 运行命令

```bash
![42716c665521cb197159054ac235a23f](D:\WX\xwechat_files\wxid_cgdpju9hrwms22_c56a\temp\RWTemp\2026-10\9e20f478899dc29eb19741386f9343c8\42716c665521cb197159054ac235a23f.png)xxxxxxxxxx # Git Bash / WSLbash scripts/smoke-test.shWITH_OUTAGE=1 bash scripts/smoke-test.shbash
```

![2c837a71004a35458733e866c6861d9f](D:\WX\xwechat_files\wxid_cgdpju9hrwms22_c56a\temp\RWTemp\2026-10\9e20f478899dc29eb19741386f9343c8\2c837a71004a35458733e866c6861d9f.png)

