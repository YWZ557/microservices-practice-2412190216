# 项目方案规划

## **目标用户**：

图书馆借阅系统（Library Borrowing System） 工程模块：monolith（单体应用，Java 25 + Spring Boot 4.0.8，基础包名 `com.zjgsu.zyw`） 代码仓库：https://github.com/YWZ557/microservices-practice-2412190216 演进路线：先以单体方式跑通业务，后续按**图书服务 / 借阅服务 / 用户服务**逐步拆分为微服务。

## 目标用户

表格

| 角色                  | 说明               | 核心诉求                                                     |
| --------------------- | ------------------ | ------------------------------------------------------------ |
| 读者（学生 / 教职工） | 图书借阅使用者     | 检索馆藏、借阅图书、归还图书、已借出时预约、查看个人借阅记录与逾期罚款 |
| 图书管理员            | 图书馆业务工作人员 | 新增 / 编辑 / 下架图书、处理借还与预约、登记损坏丢失、查看借阅统计 |
| 系统管理员            | 平台运维人员       | 管理账号与角色权限、维护基础字典数据、查看全平台运行统计     |

> 阶段 1 优先服务**读者**与**图书管理员**两类角色；系统管理员相关的认证与权限留到后续迭代。

## 优先实现的业务场景

场景编号 SC-01：图书借阅（借书 → 还书 → 逾期罚款）完整流程

1. 读者检索目标图书，系统返回馆藏状态（在馆 / 已借出）。
2. 图书在馆时，校验读者当前在借数量是否达到上限；超限则拒绝借阅。

## 两个核心模型

### 1. Book（图书 / 馆藏）

表格

| 字段                       | 类型   | 说明                                              |
| -------------------------- | ------ | ------------------------------------------------- |
| id                         | Long   | 主键                                              |
| isbn                       | String | 国际标准书号，唯一                                |
| title / author / publisher | String | 书名、作者、出版社                                |
| category                   | String | 分类                                              |
| totalCount                 | int    | 馆藏总册数                                        |
| availableCount             | int    | 可借数量                                          |
| status                     | 枚举   | AVAILABLE 在馆 / BORROWED 已借出 / REMOVED 已下架 |
| location                   | String | 馆藏位置（书架号）                                |

- 不变量：`0 <= availableCount <= totalCount`。
- 职责：承载图书档案与库存，库存只允许在借阅业务中增减，是借阅校验的第一入口。

### 2. BorrowRecord（借阅记录）

表格

| 字段        | 类型       | 说明                                 |
| ----------- | ---------- | ------------------------------------ |
| id          | Long       | 主键                                 |
| bookId      | Long       | 关联 Book.id                         |
| readerId    | Long       | 关联读者                             |
| borrowDate  | LocalDate  | 借出日期                             |
| dueDate     | LocalDate  | 应还日期                             |
| returnDate  | LocalDate  | 实际归还日期，未还为 null            |
| status      | 枚举       | BORROWED / RETURNED / OVERDUE / LOST |
| overdueDays | int        | 逾期天数                             |
| fineAmount  | BigDecimal | 罚款金额                             |



# 业务建模（第 04 周）

## 本周实现的核心业务流程

场景编号 SC-01：**图书借阅流程（借书 → 校验 → 生成借阅记录 → 库存扣减）**，本周聚焦 “借出” 这一原子业务段：

1. 读者（Reader）提交借书请求，携带 `readerId` 与 `bookId`。
2. 系统根据 `bookId` 查询图书，校验图书馆藏状态为 `AVAILABLE`（在馆）且 `availableCount > 0`。
3. 校验该读者当前在借数量是否已达到最大借阅上限（`MAX_BORROW_LIMIT`）。
4. 校验通过后，生成一条 `BORROWED` 状态的 `BorrowRecord`，并执行图书库存 `availableCount - 1`。
5. 返回借阅成功，记录 `borrowDate` 与 `dueDate`。

> 还书、逾期罚款、预约排队属于后续迭代，本周只落地 “借出” 这一闭环业务段。

## 参与流程的业务实体及关系

表格

| 实体                     | 职责                                         | 与其它实体关系                                               |
| ------------------------ | -------------------------------------------- | ------------------------------------------------------------ |
| Book（图书）             | 承载图书档案与馆藏库存，是借阅校验的第一入口 | 1 本 Book 对应 0..N 条 BorrowRecord（一对多）                |
| BorrowRecord（借阅记录） | 记录一次借出 / 归还的完整生命周期与罚款信息  | 多条 BorrowRecord 归属 1 本 Book（多对一），并关联 1 名 Reader |

### 需要在 Service 中检查的业务规则

- **规则 BR-01（借阅数量上限）**：单名读者的当前在借数量不得超过上限 `MAX_BORROW_LIMIT`（本周设为 5）。`BorrowService.borrow()` 在生成借阅记录前，必须统计该读者 `status = BORROWED` 的记录数，超限则抛出业务异常并拒绝借阅。
- **规则 BR-02（库存与状态一致性）**：仅当图书 `status = AVAILABLE` 且 `availableCount > 0` 时允许借出；借出后 `availableCount` 减 1，且始终满足不变量 `0 <= availableCount <= totalCount`。