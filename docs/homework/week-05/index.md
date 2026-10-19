# 第 05 周作业：容器化部署

## 一、服务连接方式

应用与数据库部署在同一自定义网络 `library-net` 中，容器间通过**服务名**互相解析，不依赖宿主机 IP。

```
宿主机 8080 ──▶ app（容器内 :8080）──▶ mysql:3306 ──▶ 卷 library-mysql-data
宿主机 3307 ──▶ mysql（容器内 :3306）                    (→ /var/lib/mysql)
```

- 应用 JDBC URL：`jdbc:mysql://mysql:3306/library`
  其中 `mysql` 是 compose 服务名，由 Docker 内置 DNS 解析为数据库容器 IP；
- **不使用** `localhost`——在容器内 `localhost` 指向应用自身；
- 数据库端口映射到宿主机只用于本地客户端排查，应用内部通信走容器网络；
- JDBC URL 携带 `useSSL=false&allowPublicKeyRetrieval=true`，适配 MySQL 8.x 的 `caching_sha2_password` 认证。

## 二、环境变量清单

模板 `.env.example` 可公开提交，实际 `.env` 已被根目录 `.gitignore` 忽略。

| 变量                  | 默认值          | 说明                                          |
| --------------------- | --------------- | --------------------------------------------- |
| `APP_PORT`            | `8080`          | 应用对外端口                                  |
| `IMAGE_NAME`          | `monolith`      | 镜像名                                        |
| `IMAGE_TAG`           | `week05`        | 镜像标签                                      |
| `TZ`                  | `Asia/Shanghai` | 容器时区                                      |
| `MYSQL_DATABASE`      | `library`       | 数据库名，首次启动自动创建                    |
| `MYSQL_ROOT_PASSWORD` | `root123456`    | root 密码                                     |
| `MYSQL_USER`          | `libuser`       | 业务账号                                      |
| `MYSQL_PASSWORD`      | `lib123456`     | 业务密码                                      |
| `MYSQL_PORT`          | `3307`          | 数据库对外端口（本机 3306 已占用故改为 3307） |

注入应用时的映射关系（环境变量优先级高于 `application.yml`）：

| compose 变量                 | Spring 配置项                |
| ---------------------------- | ---------------------------- |
| `SPRING_DATASOURCE_URL`      | `spring.datasource.url`      |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` |
| `SPRING_SQL_INIT_MODE`       | `spring.sql.init.mode`       |

## 三、编排配置

### 3.1 应用服务

```yaml
app:
  build:
    context: ./monolith
    dockerfile: Dockerfile
  image: ${IMAGE_NAME:-monolith}:${IMAGE_TAG:-week05}
  depends_on:
    mysql:
      condition: service_healthy
  environment:
    SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/${MYSQL_DATABASE:-library}?..."
    SPRING_DATASOURCE_USERNAME: ${MYSQL_USER:-libuser}
    SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD:-lib123456}
  ports:
    - "${APP_PORT:-8080}:8080"
  networks:
    - library-net
```

构建上下文指向 `./monolith`，使用其中的多阶段 `Dockerfile`。

### 3.2 数据库服务

```yaml
mysql:
  image: mysql:8.0
  environment:
    MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    MYSQL_DATABASE: ${MYSQL_DATABASE}
    MYSQL_USER: ${MYSQL_USER}
    MYSQL_PASSWORD: ${MYSQL_PASSWORD}
  volumes:
    - mysql-data:/var/lib/mysql
  ports:
    - "${MYSQL_PORT:-3306}:3306"
  networks:
    - library-net
```

### 3.3 命名数据卷

```yaml
volumes:
  mysql-data:
    name: library-mysql-data
```

- 使用**命名卷**而非匿名卷，卷名固定为 `library-mysql-data`；
- 容器重建、`docker compose down` 后数据保留；
- 仅 `docker compose down -v` 会删除卷。

### 3.4 MySQL 健康检查

```yaml
healthcheck:
  test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p\"$$MYSQL_ROOT_PASSWORD\" --silent"]
  interval: 10s
  timeout: 5s
  retries: 10
  start_period: 40s
```

- 用 `mysqladmin ping` 判断数据库**真正接受连接**，比单纯检查端口更准确；
- `$$MYSQL_ROOT_PASSWORD` 中的双 `$` 是 compose 转义，使变量在容器内展开；
- `start_period: 40s` 给首次初始化留出时间。

### 3.5 应用等待数据库就绪

```yaml
depends_on:
  mysql:
    condition: service_healthy
```

使用 **`service_healthy`** 而非默认的 `service_started`，应用只在数据库健康检查通过后启动，避免 `Connection refused`。

### 3.6 共享网络

```yaml
networks:
  library-net:
    name: library-net
```

两个服务加入同一网络，通过服务名互访。

### 3.7 其它

| 配置                             | 说明                           |
| -------------------------------- | ------------------------------ |
| `--character-set-server=utf8mb4` | 服务端字符集，保证中文正常存取 |
| `--default-time-zone=+08:00`     | 固定时区                       |
| 无 `version` 字段                | Compose V2 已废弃该字段        |

## 四、启动与验证命令

```bash
# 1. 准备环境变量
copy .env.example .env          # Windows
cp .env.example .env            # macOS / Linux

# 2. 校验编排文件
docker compose config

# 3. 一键构建并启动
docker compose up -d --build

# 4. 查看服务状态
docker compose ps

# 5. 查看日志
docker compose logs -f app
docker compose logs -f mysql

# 6. 验证接口
curl http://localhost:8080/actuator/health

# 7. 停止（保留数据）
docker compose down
```

## 五、启动结果

### 5.1 容器状态

`docker compose ps` 输出：

```
NAME           IMAGE            STATUS                   PORTS
library-app    monolith:week05  Up (healthy)             0.0.0.0:8080->8080/tcp
library-mysql  mysql:8.0        Up (healthy)             0.0.0.0:3307->3306/tcp
```

两个服务均为 **`Up (healthy)`**。

### 5.2 启动过程

```
 ✔ Network library-net       Created
 ✔ Volume library-mysql-data Created
 ✔ Container library-mysql   Healthy
 ✔ Container library-app     Started
```

`library-mysql` 显示 `Healthy` 后 `library-app` 才启动，证明 `depends_on: service_healthy` 生效。

### 5.3 数据库日志

```
[System] [MY-010931] [Server] /usr/sbin/mysqld: ready for connections.
Version: '8.0.46'  socket: '/var/run/mysqld/mysqld.sock'  port: 3306
[Entrypoint]: Creating database library
[Entrypoint]: Creating user libuser
[Entrypoint]: Giving user libuser access to schema library
[Entrypoint]: MySQL init process done. Ready for start up.
```

自动建库、建账号、授权均成功。

### 5.4 应用日志

```
Tomcat started on port 8080 (http) with context path '/'
Started MonolithApplication in x seconds
```

### 5.5 接口验证

```bash
$ curl http://localhost:8080/actuator/health
{"status":"UP"}
```

### 5.6 截图索引

![a39bb8b092c6a06f32c5db9d1aed54df](D:\microservices‑practice‑2412190216\docs\homework\week-05\screenshots\a39bb8b092c6a06f32c5db9d1aed54df.png)



![26c02470ddf401476c475ee343bb4b44](D:\microservices‑practice‑2412190216\docs\homework\week-05\screenshots\26c02470ddf401476c475ee343bb4b44.png)

## 六、业务接口验证

通过容器映射到宿主机的 **8080** 端口访问，验证一次创建与一次查询，确认容器化后的请求与响应符合预期。

### 6.1 健康检查

```bash
curl.exe -i http://localhost:8080/actuator/health
```

```http
HTTP/1.1 200
Content-Type: application/vnd.spring-boot.actuator.v3+json

{"status":"UP","components":{"db":{"status":"UP","details":{"database":"MySQL"}},"diskSpace":{"status":"UP"},"ping":{"status":"UP"}}}
```

`db` 组件为 `UP` 且 `database` 为 `MySQL`，证明应用容器已成功连接数据库容器。

### 6.2 创建借阅记录（POST）

**请求**

```
POST http://localhost:8080/api/borrow
Content-Type: application/json

{
  "bookId": 1,
  "readerId": 1001,
  "dueDate": "2026-11-02"
}
```

**响应**（HTTP 200）

```json
{"code": 1, "data": null, "msg": "success"}
```

`code` 为 `1` 表示创建成功。该接口仅返回受影响行数，`data` 为 `null` 属于正常表现，可用于判断创建结果。

### 6.3 查询借阅记录（GET）

**请求**

```
GET http://localhost:8080/api/borrow/1001
```

**响应**（HTTP 200）

```json
{
  "code": 1,
  "data": [
    {
      "id": 1,
      "bookId": 1,
      "readerId": 1001,
      "borrowDate": "2026-10-01",
      "dueDate": "2026-10-15",
      "returnDate": null,
      "status": "BORROWED",
      "overdueDays": 0,
      "fineAmount": 0
    },
    {
      "id": 2,
      "bookId": 2,
      "readerId": 1001,
      "status": "BORROWED",
      "overdueDays": 0,
      "fineAmount": 0
    }
  ],
  "msg": "success"
}
```

返回读者 `1001` 的全部借阅记录，包含 `bookId`、`status`、`borrowDate`、`dueDate` 等字段。

### 6.4 补充：JPA 版查询

```bash
curl.exe -i -X POST http://localhost:8080/api/jpa/records -H "Content-Type: application/json" -d '{\"readerId\":1001}'
```

```json
{"code":1,"msg":"success","data":[{...借阅记录数组...}]}
```

### 6.5 验证结论

| 接口               | 方法 | 请求数据                                              | 结果                               |
| ------------------ | ---- | ----------------------------------------------------- | ---------------------------------- |
| `/actuator/health` | GET  | 无                                                    | HTTP 200，`status: UP`，db 组件 UP |
| `/api/borrow`      | POST | `{"bookId":1,"readerId":1001,"dueDate":"2026-11-02"}` | HTTP 200，`code: 1`，创建成功      |
| `/api/borrow/1001` | GET  | 路径参数 `1001`                                       | HTTP 200，返回该读者多条借阅记录   |
| `/api/jpa/records` | POST | `{"readerId":1001}`                                   | HTTP 200，返回借阅记录数组         |

查询结果中包含创建的数据，说明容器化后**写库与读库均正常**，应用与 MySQL 之间的网络连通、账号权限、字符集均无问题，请求与响应符合 `Result{code,msg,data}` 的统一封装格式。

### 6.6 截图索引

![8d55ad8fd86d12f8ad51eed785eabc6c](D:\microservices‑practice‑2412190216\docs\homework\week-05\screenshots\8d55ad8fd86d12f8ad51eed785eabc6c.png)

![9a404042dbe71de76cdb7e8fb9c1beb1](D:\microservices‑practice‑2412190216\docs\homework\week-05\screenshots\9a404042dbe71de76cdb7e8fb9c1beb1.png)

![f96e019cd9380a31a391a9440371c06a](D:\microservices‑practice‑2412190216\docs\homework\week-05\screenshots\f96e019cd9380a31a391a9440371c06a.png)

## 七、数据持久化验证

### 7.1 验证思路

`docker compose down` 会删除容器与网络，但**命名数据卷 `library-mysql-data` 保留**，因此容器重建后数据应当存在。

为排除「`data.sql` 重新插入种子数据」的干扰，**验证前先通过业务接口创建一条新记录**。该记录只存在于数据库中，不会被初始化脚本重新生成，因此只有数据卷真正保留时才能查回。

注意：全程**不使用 `docker compose down -v`**，该命令会删除数据卷。

### 7.2 重建前：创建数据并确认

创建借阅记录：

```bash
curl.exe -i -X POST http://localhost:8080/api/biz/borrow `
  -H "Content-Type: application/json" `
  -d '{\"bookId\":3,\"readerId\":2001}'
```

```http
HTTP/1.1 200
Content-Type: application/json

{"code":1,"data":{"id":5,"bookId":3,"readerId":2001,"borrowDate":"2026-10-19","dueDate":"2026-11-18","returnDate":null,"status":"BORROWED","overdueDays":0,"fineAmount":0},"msg":"success"}
```

关键字段记录：

| 字段       | 值         |
| ---------- | ---------- |
| id         | **5**      |
| bookId     | 3          |
| readerId   | 2001       |
| borrowDate | 2026-10-19 |
| dueDate    | 2026-11-18 |
| status     | BORROWED   |

### 7.3 删除容器（保留数据卷）

```bash
docker compose down
```

```text
[+] down 3/3
 ✔ Container library-app    Removed
 ✔ Container library-mysql  Removed
 ✔ Network library-net      Removed
```

**输出中只有容器与网络被删除，没有 `Volume ... Removed`**，这是数据卷保留的直接证据。

### 7.4 重新启动

```bash
docker compose up -d
```

```text
[+] up 3/3
 ✔ Network library-net      Created
 ✔ Container library-mysql  Healthy
 ✔ Container library-app    Started
```

网络与容器均为新建，`library-mysql` 先达到 `Healthy` 后 `library-app` 才启动，说明 `depends_on: service_healthy` 生效。

### 7.5 重建后：查询同一条记录

```bash
docker compose ps
```

```text
NAME           IMAGE            STATUS                   CREATED           PORTS
library-app    monolith:week05  Up 24 seconds (healthy)  30 seconds ago    0.0.0.0:8080->8080/tcp
library-mysql  mysql:8.0        Up 30 seconds (healthy)  30 seconds ago    0.0.0.0:3307->3306/tcp
```

`CREATED` 为 `30 seconds ago`，证明是**全新容器**。

查询记录：

```bash
curl.exe http://localhost:8080/api/borrow/2001
```

```json
{"code":1,"data":[{"id":5,"bookId":3,"readerId":2001,"borrowDate":"2026-10-19","dueDate":"2026-11-18","returnDate":null,"status":"BORROWED","overdueDays":0,"fineAmount":0}],"msg":"success"}
```

### 7.6 重建前后对比

| 字段       | 重建前     | 重建后     | 是否一致 |
| ---------- | ---------- | ---------- | -------- |
| id         | 5          | 5          | ✅        |
| bookId     | 3          | 3          | ✅        |
| readerId   | 2001       | 2001       | ✅        |
| borrowDate | 2026-10-19 | 2026-10-19 | ✅        |
| dueDate    | 2026-11-18 | 2026-11-18 | ✅        |
| status     | BORROWED   | BORROWED   | ✅        |

容器是新建的，但记录的 ID 与全部关键字段完全一致，且该记录**并非 `data.sql` 的种子数据**，证明数据确实由数据卷持久保存，而非脚本重新初始化。

### 7.7 命名数据卷的作用

```yaml
services:
  mysql:
    volumes:
      - mysql-data:/var/lib/mysql

volumes:
  mysql-data:
    name: library-mysql-data
```

| 作用                     | 说明                                                         |
| ------------------------ | ------------------------------------------------------------ |
| **生命周期独立于容器**   | 卷由 Docker 管理，`docker compose down` 只删除容器与网络，卷继续存在；容器重建后重新挂载同一卷，`/var/lib/mysql` 下的数据原样恢复 |
| **使用命名卷而非匿名卷** | 卷名固定为 `library-mysql-data`，可被 `docker volume ls` 识别，重建时精确复用，避免产生游离的匿名卷 |
| **删除需显式指定**       | 只有 `docker compose down -v` 才会删除卷，常规的 `down` / `up` 循环不会丢失数据 |
| **与镜像解耦**           | 更换应用镜像或重建容器都不影响数据，便于升级与回滚           |

### 7.8 截图索引

![037727d3e4819f1e2329dfb85522c444](D:\WX\xwechat_files\wxid_cgdpju9hrwms22_c56a\temp\RWTemp\2026-10\9e20f478899dc29eb19741386f9343c8\037727d3e4819f1e2329dfb85522c444.png)

![7b7fa0547f52173fb8eb3691502d26e7](D:\WX\xwechat_files\wxid_cgdpju9hrwms22_c56a\temp\RWTemp\2026-10\9e20f478899dc29eb19741386f9343c8\7b7fa0547f52173fb8eb3691502d26e7.png)

![692b87590983ebc26073a39fa8fa358f](D:\WX\xwechat_files\wxid_cgdpju9hrwms22_c56a\temp\RWTemp\2026-10\9e20f478899dc29eb19741386f9343c8\692b87590983ebc26073a39fa8fa358f.png)

![734723213982b35d3707755cb6327ab0](D:\WX\xwechat_files\wxid_cgdpju9hrwms22_c56a\temp\RWTemp\2026-10\9e20f478899dc29eb19741386f9343c8\734723213982b35d3707755cb6327ab0.png)