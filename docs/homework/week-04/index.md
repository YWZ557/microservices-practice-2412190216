# 第 04 周作业

## 业务建模

### 实现思路

本周聚焦图书馆借阅系统「图书借阅（借出）」核心业务段，明确参与流程的实体、实体关系与 Service 业务规则：

- **核心流程**：读者发起借书 → 校验图书在馆（AVAILABLE 且 availableCount > 0）→ 校验读者在借数量未超上限（MAX_BORROW_LIMIT = 5）→ 生成 BORROWED 借阅记录 → 库存 availableCount 减 1。
- **业务实体**：Book（图书，承载馆藏库存）与 BorrowRecord（借阅记录，记录借出/归还生命周期），二者为一对多关系。
- **Service 业务规则**：BR-01 借阅数量上限校验、BR-02 库存与状态一致性校验。

### 运行命令

```bash
cd monolith
./mvnw.cmd clean package -DskipTests
./mvnw.cmd test
```

### 结果

工程可正常编译、打包并通过单元测试；核心流程、实体关系与业务规则均已明确，作为后续实体类与 Service 层开发的设计依据。

## 数据库持久化

### 实现思路

在单体工程中接入 MySQL 与 Spring Data JPA：

1. 引入 `spring-boot-starter-data-jpa` 与 `mysql-connector-j` 依赖；
2. 定义 `Book`、`BorrowRecord` 两个 JPA 实体（位于 `pojo` 包），用 `@Column` 映射下划线字段；
3. 在 `repository` 包编写 `BookRepository`、`BorrowRecordRepository`（继承 JpaRepository）；
4. 使用 DTO（`BorrowQueryRequest`）承载 API 输入；
5. 用 `data.sql`（幂等插入）提供可重复执行的测试数据。

### 运行命令

```bash
cd monolith
./mvnw.cmd clean package -DskipTests
./mvnw.cmd spring-boot:run
./mvnw.cmd test
```

### 结果

应用启动后连接 MySQL，通过 `data.sql` 建表并幂等导入测试数据；`/api/jpa` 系列接口可正常读写图书与借阅记录，数据库持久化链路完整可用。

## 测试与验证

### 实现思路

使用 H2 内存库 + Spring Boot Test 编写三类测试，覆盖 Repository 读写、Service 业务规则、API 正常/错误响应：

1. `BookRepositoryTest`：验证 Repository 的读（findAll/findById）与写（save）能力；
2. `BorrowBizServiceTest`：验证 BR-01（借阅上限）与 BR-02（在馆校验）两条业务规则；
3. `BorrowBizControllerTest`：用 MockMvc 验证 `/api/biz/borrow` 的正常（code=1）与错误（code=0）响应。

测试数据在 `@BeforeEach` 中通过 Repository 准备，`ddl-auto: create-drop` + H2 内存库保证每次测试独立、可重复执行。


