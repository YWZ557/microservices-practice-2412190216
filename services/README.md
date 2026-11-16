# services：Monolith（Feign + Nacos）容器化交付

本目录现在是四个独立模块（`library-user` / `library-book` / `library-borrow` /
`library-gateway`）的 Feign 实现，各模块自带 POM，不再依赖 `monolith-common` / `library-api`。
旧的 `user-service` / `book-service` / `borrow-service` RestTemplate 版本已删除，
容器运行参数全部通过 compose 环境变量覆盖。

## 1. 目录

```text
services/
├── library-user/             # 8081，独立 POM
├── library-book/             # 8082，独立 POM
├── library-borrow/           # 8083，独立 POM（Feign 客户端内联）
├── library-gateway/          # 8080，独立 POM
├── pom.xml                   # 仅聚合 4 个模块，不做依赖管理
├── db/01-library-user.sql    # 读者库 library_user
├── db/02-library-book.sql    # 图书库 library_book
├── db/03-library-borrow.sql  # 借阅库 library_borrow
├── Dockerfile.monolith       # 四个模块共用构建文件（ARG MODULE）
├── compose.yaml              # MySQL + Nacos + 4 个业务服务
├── .env.example
└── scripts/e2e-test.sh
```

## 2. 一键启动

```bash
cd services
# 本工作区已生成可直接运行的 .env；若 .env 不存在，再执行：
# cp .env.example .env
# 编辑 .env：至少填 NACOS_AUTH_TOKEN / NACOS_AUTH_IDENTITY_KEY / NACOS_AUTH_IDENTITY_VALUE
# 生成 Nacos token：openssl rand -base64 32
docker compose up -d --build --remove-orphans
docker compose ps
```

首次启动后打开 Nacos 控制台：

```text
http://127.0.0.1:18080
```

Nacos 3 首次访问需要初始化管理员，用户名填 `nacos`，密码填 `.env` 里的
`NACOS_PASSWORD`（默认 `123456`）。各模块 `application.yaml` 中写的是
`nacos/123456`，如果不一致，需要改 `.env` 或控制台密码。

如果业务服务先于管理员初始化启动，日志出现 `403 / user not found`，初始化完成后
重启一次业务服务即可重新注册：

```bash
docker compose restart user-service book-service borrow-service gateway
```

> 原 `services/monolith/compose.yaml` 只是 Nacos-only 文件，已随目录拆分移除；
> 新交付统一使用本目录的 `services/compose.yaml`。

## 3. 端口

| 服务 | 宿主机 | 容器内 | 说明 |
|---|---:|---:|---|
| gateway | 8080 | 8080 | 统一入口 |
| user-service | 8081 | 8081 | 读者/登录 |
| book-service | 不映射 | 8082 | 两个副本，由 Feign LoadBalancer 轮询 |
| borrow-service | 8083 | 8083 | Feign 编排 |
| mysql | 3308 | 3306 | 三个库：`library_user` / `library_book` / `library_borrow` |
| nacos 控制台 | 18081 | 8080 | Nacos 3 |
| nacos HTTP | 8849 | 8848 | 注册/发现 |
| nacos gRPC | 9850 | 9848 | 客户端长连接 |

本机 3307 / 18080 / 8848 / 9848 / 9849 已被其他容器占用，`.env` 中整体后移一位；
换到空闲端口只需改 `.env` 的 `MYSQL_PORT`、`NACOS_*_PORT`，容器内部端口不变：

```bash
# 例如恢复默认端口
MYSQL_PORT=3307 docker compose up -d --build
```

## 4. 数据库

`db/` 下按服务拆成三个独立初始化脚本，MySQL 容器首次启动时按文件名顺序执行：

| 脚本 | 数据库 | 账号 / 密码 | 表 |
|---|---|---|---|
| `01-library-user.sql` | `library_user` | `user_svc` / `user_pass` | `reader` |
| `02-library-book.sql` | `library_book` | `book_svc` / `book_pass` | `book` |
| `03-library-borrow.sql` | `library_borrow` | `borrow_svc` / `borrow_pass` | `borrow_record` |

每个服务在 compose 里用 `SKY_DATASOURCE_DATABASE/USERNAME/PASSWORD` 指向自己的库；
`application-dev.yml` 里的 `library` + `root/123` 只在本地直连 3307 时生效，
容器内被环境变量覆盖。

手工核对：

```bash
docker compose exec -T mysql mysql -uroot -proot123456 -e \
  "SELECT COUNT(*) FROM library_book.book;
   SELECT COUNT(*) FROM library_borrow.borrow_record;
   SELECT COUNT(*) FROM library_user.reader;"
```

> 初始化脚本只在数据卷为空时执行。改过脚本后必须先 `docker compose down -v` 再
> `up`，否则改动不会生效。

## 5. 端到端验证

```bash
bash scripts/e2e-test.sh
```

脚本覆盖：

1. 正常借书：`code=1`，`borrow_record` +1，`book.status=BORROWED`
2. 重复借阅：`code=0`，提示“无可借库存”，记录数和状态不变
3. 图书不存在：`code=0`，提示“图书不存在”，无记录
4. 停止 `book-service`：`code=0`，提示“不可用”，记录数和状态不变
5. 恢复 `book-service`：重新借书成功，记录数和状态正确

每个失败场景都查 `library_borrow.borrow_record` 与 `library_book.book`，不只断言
`code/msg`。

## 6. 与根目录的关系

根目录 `compose.yaml` 已改为 include 本文件：

```yaml
name: library
include:
  - path: services/compose.yaml
```

因此下面两条命令等价：

```bash
# 仓库根目录
docker compose up -d --build --remove-orphans

# services 目录
cd services
docker compose up -d --build --remove-orphans
```

根 `README.md` 里旧的 `monolith` / `app + mysql` 容器化段落属于历史内容，
不再对应当前 compose。
