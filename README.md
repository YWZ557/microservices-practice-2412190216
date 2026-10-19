微服务课程作业仓库 - 

课程：微服务开发与实践

 姓名：张跃威

学号：2412190216

仓库用途：存放微服务课程每周作业、代码与文档

## 图书馆借阅系统

### 项目简介

图书馆借阅系统是面向校园场景的图书借阅管理平台，用于实现图书信息管理、读者借阅、归还、预约、逾期罚款等全套图书流转业务，替代传统人工登记模式，实现图书馆日常管理数字化、规范化。

### 业务背景

传统校园图书馆依赖人工登记图书借还信息，操作繁琐、效率低下，容易出现借阅记录丢失、图书库存混乱、逾期罚款统计不准确等问题。学生无法自主查询图书馆藏状态和借阅记录，管理员统计图书借阅数据工作量大，需一套系统化的借阅管理平台统一管理图书资源与借阅流程。

### 目标用户

1. 读者（学生/教职工）：查询图书、借阅图书、归还图书、预约图书、查看借阅记录与逾期罚款。
2. 图书管理员：维护图书信息、处理借还业务、管理图书库存、处理读者预约与违规记录。 
3. 系统管理员：管理系统账号、分配权限、统计平台借阅数据、维护系统基础信息。

## 项目目标和适用场景

### 项目目标

实现校园图书馆图书资源数字化管理，规范化图书借阅、归还、预约、罚款全流程业务；区分多角色权限，记录完整业务流水，降低人工管理成本。作为本学期持续演进的后端课程项目，逐步学习持久化、事务、消息、认证、监控、部署等后端技术。

### 适用场景

高校校内图书馆，服务在校学生、教职工与图书馆工作人员，仅处理纸质图书的馆藏与借阅业务。

## 主要用户角色及各角色需求

表格

| 用户角色              | 角色说明           | 核心需求                                                     |
| --------------------- | ------------------ | ------------------------------------------------------------ |
| 读者（学生 / 教职工） | 图书借阅使用者     | 查询馆藏图书信息、借阅图书、归还图书；当图书已借出时发起预约；查看个人借阅记录、逾期信息与罚款；缴纳逾期罚款 |
| 图书管理员            | 图书馆业务工作人员 | 新增、编辑、下架图书；处理读者借书、还书业务；管理预约队列；登记图书损坏 / 丢失；查看借阅统计 |
| 系统管理员            | 平台运维管理人员   | 管理全部账号、分配角色权限；维护基础字典数据；查看全平台运行统计，管理系统基础配置 |

## 功能模块划分

1. **用户账号模块**
   - 账号注册、登录；
   - 用户信息修改；
   - 角色权限分配（读者 / 图书管理员 / 系统管理员）。
2. **图书信息模块**
   - 图书新增、信息编辑、图书下架；
   - 图书库存管理；
   - 图书多条件检索（书名、作者、ISBN）。
3. **借阅管理模块**
   - 发起借书，生成借阅记录，图书库存 - 1；
   - 归还图书，关闭借阅记录，图书库存 + 1；
   - 校验读者最大可借数量。
4. **图书预约模块**
   - 图书在馆时不可预约，图书借出后读者可预约；
   - 预约排队，图书归还后通知首位预约读者；
   - 预约超时自动失效。
5. **逾期罚款模块**
   - 归还图书时自动计算逾期天数与罚款金额；
   - 罚款记录保存、罚款缴纳状态管理。
6. **后台统计模块**
   - 图书借阅热度统计；
   - 逾期未还图书统计；
   - 读者借阅数据报表。

## 核心业务流程：图书借阅完整流程

A [读者登录系统] --> B [检索目标图书] B --> C {图书在馆？} C -- 否 --> D [读者提交预约，进入预约队列] C -- 是 --> E {读者是否达到最大借书上限？} E -- 是 --> F [拒绝借阅，提示上限] E -- 否 --> G [管理员确认借阅] G --> H [新增借阅记录，图书库存减 1] H --> I [借阅成功，记录应还日期]

业务文字描述： 读者登录系统检索图书，系统判断图书馆藏状态。如果图书已被借出，则读者可提交预约进入排队；如果图书在馆，校验读者当前借阅数量，若未超过最大借书限额，管理员确认借书，系统生成借阅记录，图书库存减一，记录应还日期，借阅完成。



## 当前项目范围 & 暂不实现内容

##### 当前范围（阶段 1，课程初期）

- 实体类建模：图书、读者、借阅记录、预约、罚款；
- Service 业务层，使用内存集合模拟数据库；
- 实现图书查询、借书、还书、预约、逾期罚款基础业务逻辑；
- 多角色业务逻辑区分。

**暂不实现**

- 前端页面；
- 真实数据库；
- 密码加密、登录认证；
- 邮件 / 短信消息推送；
- 复杂可视化报表。

## 后续迭代扩展方向（本学期逐步开发）

1. **认证与权限**：接入登录认证，基于角色的访问控制，不同角色接口权限隔离。
2. **数据库持久化**：接入 MySQL，将内存数据迁移到数据库，设计图书、借阅、预约数据表。
3. **事务控制**：借书、还书业务添加数据库事务，保证库存和借阅记录的原子性，防止数据不一致。
4. **消息通知**：当被预约图书归还、图书逾期时，发送消息通知读者。
5. **服务拆分**：按业务拆分模块，图书服务、借阅服务、用户服务。
6. **监控与部署**：增加日志、异常监控；项目打包，完成服务部署。

## 运行环境与操作命令

### 一、环境要求

| 工具     | 版本要求   | 说明                                                         |
| :------- | :--------- | :----------------------------------------------------------- |
| JDK      | Java 25    | 工程pom.xml指定java.version=25，项目编译、运行必须使用JDK25； |
| Maven    | 3.9 及以上 | 项目内置Maven Wrapper 3.9.16                                 |
| 服务端口 | 8080       | 项目默认端口为8080，启动前需确认该端口未被其他程序占用，避免端口冲突导致启动失败 |

### 二、启动与测试命令

所有操作命令均需在项目根目录下的 `monolith/` 目录内执行，区分Windows、macOS/Linux系统：

```Plain
# 进入项目工程目录
cd monolith

# Windows系统：切换JDK25环境（非默认环境必执行）
$env:JAVA_HOME = "D:\JAVA25"

# 1. 编译打包（跳过测试，快速构建项目）
# Windows
./mvnw.cmd clean package -DskipTests
# macOS / Linux
./mvnw clean package -DskipTests

# 2. 启动项目（两种方式二选一）
# 方式一：运行打包后的jar包
java -jar target/monolith-0.0.1-SNAPSHOT.jar
# 方式二：Maven插件直接启动
# Windows
./mvnw.cmd spring-boot:run
# macOS / Linux
./mvnw spring-boot:run

# 3. 执行项目单元测试（验证应用上下文可正常加载）
# Windows
./mvnw.cmd test
# macOS / Linux
./mvnw test
```

### 三、接口验证地址

- 项目测试GET接口：`http://localhost:8080/api/hello`
- 系统健康检查接口：`http://localhost:8080/actuator/health`，正常返回状态为 UP

## 一、MySQL 安装（本机已装好）

| 项目       | 实际情况                                                    |
| ---------- | ----------------------------------------------------------- |
| 版本       | MySQL **8.0.46** Community Server                           |
| 安装目录   | `C:\Program Files\MySQL\MySQL Server 8.0`                   |
| 服务名     | `MySQL80`（Windows 服务，当前状态 **Running**，已开机自启） |
| 客户端命令 | `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`     |

- 

------

## 二、建库（`library` 库已存在）

我查过了，**`library` 数据库已经建好**，里面已经有表和数据：

- `book` 表：**5 条**数据
- `borrow_record` 表：**6 条**数据



sql



```
CREATE DATABASE IF NOT EXISTS library
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
```

命令行执行方式：

powershell



```
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS library DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

------

## 三、连接配置

`application.yml` 当前配置是：

yaml



```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/library
    username: "root"
    password: "123456"
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## 容器化部署

### 1. 准备环境变量
copy .env.example .env        # Windows
cp .env.example .env          # macOS/Linux

### 2. 一键启动
docker compose up -d --build

### 3. 查看状态
docker compose ps

### 4. 查看日志
docker compose logs -f app

### 5. 停止服务（保留数据）
docker compose down

### 6. 停止并清除数据卷
docker compose down -v

------

## 容器化部署（Docker Compose）

编排文件 `compose.yaml` 与应用镜像 `monolith/Dockerfile` 均位于仓库中，可一键构建并启动「Spring Boot 应用 + MySQL」两个服务。

### 一、准备环境变量

```bash
# Windows
copy .env.example .env
# macOS / Linux
cp .env.example .env
```

> `.env` 含真实密码，已被根目录 `.gitignore` 忽略，不要提交到 Git。
> 本机已安装 MySQL 占用 3306 时，将 `.env` 中的 `MYSQL_PORT` 改为 `3307`。

### 二、一键构建并启动

```bash
docker compose up -d --build
```

### 三、查看状态

```bash
docker compose ps
```

两个容器均应为 `Up (healthy)`：

```
NAME           IMAGE            STATUS                   PORTS
library-app    monolith:week05  Up (healthy)             0.0.0.0:8080->8080/tcp
library-mysql  mysql:8.0        Up (healthy)             0.0.0.0:3307->3306/tcp
```

### 四、查看日志

```bash
# 实时跟踪应用日志
docker compose logs -f app

# 查看数据库日志
docker compose logs --tail 30 mysql
```

### 五、停止服务

```bash
# 停止并保留数据
docker compose down

# 停止并删除数据卷（数据会丢失，慎用）
docker compose down -v
```

------

## 接口验证（容器运行后）

> PowerShell 中请使用 `curl.exe`，直接写 `curl` 会被解析为 `Invoke-WebRequest` 别名。

### 1. 健康检查

```bash
curl.exe -i http://localhost:8080/actuator/health
```

预期返回：

```http
HTTP/1.1 200

{"status":"UP","components":{"db":{"status":"UP","details":{"database":"MySQL"}}}}
```

`db` 组件为 `UP` 且 `database` 为 `MySQL`，说明应用已成功连接数据库容器。

### 2. 查询图书列表

```bash
curl.exe -i http://localhost:8080/api/jpa/books
```

预期返回 `code` 为 `1`，`data` 为图书数组：

```json
{"code":1,"msg":"success","data":[{"id":1,"title":"Java 编程思想","availableCount":3}]}
```

### 3. 创建借阅记录（借书）

```bash
curl.exe -i -X POST http://localhost:8080/api/biz/borrow -H "Content-Type: application/json" -d '{\"bookId\":1,\"readerId\":2001}'
```

预期返回：

```json
{"code":1,"msg":"success","data":{"bookId":1,"readerId":2001,"status":"BORROWED"}}
```

> 业务规则：图书须为 `AVAILABLE` 且有库存；同一读者在借数量上限为 5 本。

### 4. 按读者查询借阅记录（JPA 版）

```bash
curl.exe -i -X POST http://localhost:8080/api/jpa/records -H "Content-Type: application/json" -d '{\"readerId\":2001}'
```

预期返回该读者的借阅记录数组，包含步骤 3 创建的记录。

### 5. 按读者查询借阅记录（MyBatis 版）

```bash
curl.exe -i http://localhost:8080/api/borrow/1001
```

预期返回读者 `1001` 的全部借阅记录。

### 6. 基础接口

```bash
curl.exe -i http://localhost:8080/api/hello
```