# Activation Code Trade Platform 激活码交易平台

> 项目缩写：**ACTrade / actrade**

## 1. 项目概述

ACTrade 是一个面向**中小型软件开发团队、独立开发者、软件版权方/授权方**的激活码交易与托管平台。

平台聚焦两类核心能力：

1. **激活码库存发货**
   - 商家上传一批激活码
   - 平台将激活码封装为商品进行销售
   - 用户下单支付后，系统自动从库存中分配激活码并发货

2. **激活工具托管生成**
   - 商家上传激活工具
   - 平台托管该工具并在订单支付成功后触发执行
   - 工具返回激活码或授权结果，平台完成发货与记录留存

本项目定位为一个**个人全栈工程项目**，目标不是只做 Demo，而是做成一套具备完整开发边界、可持续迭代、可容器化部署的中小型 Web 系统。

---

## 2. 范围

本项目采用以下约束：

### 2.1 前端范围
- 前端只做 **Web 开发**
- 不包含：
  - 小程序
  - Android / iOS App
  - 桌面客户端

### 2.2 后端技术基线
- Java：**21**
- Spring：**Spring Boot 3.5.10（最新稳定版）**
- Spring Framework：跟随 Spring Boot 官方依赖管理
- 构建工具：Maven

### 2.3 数据与中间件
- PostgreSQL：**16**
- Redis：缓存与轻量级状态控制
- RabbitMQ：异步任务与延迟任务

### 2.4 激活码业务范围
本项目只支持以下两种激活交付模式：

#### 模式 A：上传激活码库存
商家直接上传激活码清单，平台按库存商品模式发货。

#### 模式 B：上传激活工具
商家上传激活工具，平台托管并在订单支付后执行该工具，生成激活结果。

### 2.5 明确不做
- 不做商家 API 调用模式
- 不做第三方激活服务回调模式
- 不做微服务拆分
- 不做多租户
- 不做复杂推荐算法
- 不做移动端

---

## 3. 项目目标

## 3.1 业务目标
构建一个完整的激活码交易闭环：

**商品展示 -> 下单 -> 支付 -> 发货 -> 查看激活码/授权结果 -> 售后记录**

## 3.2 技术目标
完成一个具备如下能力的工程项目：

- 前后端分离
- Web 端完整页面开发
- Java 21 + Spring Boot 4 的企业风格后端
- PostgreSQL / Redis / RabbitMQ 综合使用
- Docker 容器化部署
- 单台服务器可运行
- 支持后续演进

## 3.3 项目成果目标
最终产出应包括：

- 一套可运行的 Web 系统
- 一套数据库结构
- 一套后端 API
- 一套后台管理能力
- 一套容器化部署方案
- 一份完整的项目文档

---

## 4. 项目定位与边界

## 4.1 项目定位
本项目是一个**数字商品电商 + 激活业务托管平台**。

它的本质不是简单商城，而是一个带有“发货策略”和“执行任务”的数字授权交易系统。

## 4.2 核心特点
- 商品是“软件激活资格”
- 发货对象是“激活码或授权结果”
- 发货方式分为“库存直发”和“工具执行生成”
- 平台具备审核、发货、记录、追踪、审计能力

## 4.3 边界说明
本项目仅用于：
- 合法软件授权分发
- 合法激活工具托管
- 学习与工程实践

不支持：
- 破解工具分发
- 未授权软件激活
- 非法注册码交易
- 绕过版权保护的行为

---

## 5. 用户角色设计

## 5.1 普通用户
权限与功能：
- 注册 / 登录
- 浏览商品
- 下单支付
- 查看订单
- 查看激活码或授权结果
- 查看历史购买记录

## 5.2 商家 / 开发者
权限与功能：
- 提交入驻申请
- 创建商品
- 上传激活码库存
- 上传激活工具
- 查看订单与发货记录
- 查看商品销量与执行情况

## 5.3 平台管理员
权限与功能：
- 审核商家
- 审核商品
- 审核激活工具
- 管理订单
- 处理异常发货
- 查看日志与风控记录
- 管理系统配置

## 5.4 角色枚举建议
- `ROLE_USER`
- `ROLE_MERCHANT`
- `ROLE_ADMIN`
- `ROLE_SUPER_ADMIN`

---

## 6. 核心业务模式

## 6.1 模式一：激活码库存发货

### 业务定义
商家预先上传一批激活码，作为商品库存。用户支付成功后，系统直接从库存中分配一条或多条激活码给用户。

### 特点
- 实现简单
- 发货快
- 成本低
- 容易做 MVP

### 适用场景
- 成品注册码
- 批量序列号
- 固定库存型数字商品

---

## 6.2 模式二：激活工具托管执行

### 业务定义
商家上传一个激活工具包，平台托管保存。用户支付成功后，系统根据订单信息执行该工具，生成激活码、授权文件或授权结果。

### 特点
- 灵活性高
- 适合按单生成
- 适合动态授权
- 安全风险显著高于库存模式

### 适用场景
- 按机器信息生成授权
- 按订单动态生成密钥
- 需要工具逻辑参与的授权流程

---

## 6.3 两种模式对比

| 维度 | 激活码库存发货 | 激活工具托管执行 |
|---|---|---|
| 发货速度 | 快 | 中等 |
| 实现复杂度 | 低 | 高 |
| 风险等级 | 中 | 高 |
| 并发处理 | 简单 | 较复杂 |
| 审核难度 | 低 | 高 |
| 适合 MVP | 是 | 是，但需严格控风险 |

---

## 7. 核心业务流程

## 7.1 用户购买流程
1. 用户注册或登录
2. 浏览商品列表
3. 查看商品详情
4. 提交订单
5. 发起支付
6. 支付成功
7. 系统根据商品交付方式执行发货
8. 用户在订单详情查看激活码/授权结果
9. 订单完成

---

## 7.2 库存发货流程
1. 商家创建商品
2. 商家上传激活码库存
3. 管理员审核商品
4. 商品上架
5. 用户下单并支付
6. 系统锁定库存激活码
7. 系统发货并写入订单记录
8. 库存状态变更为已售出

---

## 7.3 激活工具执行流程
1. 商家创建商品
2. 商家上传激活工具包
3. 平台进行文件校验与人工审核
4. 管理员审核通过后商品上架
5. 用户下单并支付
6. 系统创建激活执行任务
7. RabbitMQ 投递任务
8. 后端消费任务并执行激活工具
9. 获取激活结果
10. 记录执行日志、生成发货记录
11. 用户查看授权结果

---

## 7.4 商家入驻流程
1. 商家注册账号
2. 提交商家资料
3. 提交资质材料
4. 管理员审核
5. 审核通过后获得商家权限
6. 开始创建商品、上传库存或上传工具

---

## 8. 功能模块设计

## 8.1 用户端功能

### 8.1.1 账户中心
- 用户注册
- 用户登录
- 退出登录
- 修改密码
- 找回密码
- 修改个人资料
- 绑定邮箱
- 绑定手机号
- 查看安全设置

### 8.1.2 商品中心
- 商品列表
- 商品详情
- 商品分类
- 商品搜索
- 价格排序
- 最新上架
- 热门推荐（先用简单规则）

### 8.1.3 下单与支付
- 购物车（可选）
- 立即购买
- 创建订单
- 订单确认
- 支付页面
- 支付结果页
- 订单取消

### 8.1.4 订单中心
- 我的订单
- 订单详情
- 订单状态查询
- 激活码查看
- 授权结果查看
- 发货记录查看

---

## 8.2 商家端功能

### 8.2.1 商家入驻
- 提交团队/公司资料
- 提交授权/资质信息
- 查看审核进度

### 8.2.2 商品管理
- 创建商品
- 编辑商品
- 设置商品价格
- 上下架商品
- 设置交付模式
- 查看库存或执行状态

### 8.2.3 激活码库存管理
- 批量上传激活码
- 查看库存数量
- 查看已售出数量
- 查看已作废数量
- 导出库存记录（可选）
- 作废指定激活码

### 8.2.4 激活工具管理
- 上传工具包
- 维护工具版本
- 设置执行参数
- 设置超时时间
- 查看执行日志
- 查看失败记录
- 手动重试失败任务

### 8.2.5 数据统计
- 订单统计
- 销售金额统计
- 商品销量统计
- 工具执行成功率
- 激活码库存消耗统计

---

## 8.3 管理后台功能
- 用户管理
- 商家审核
- 商品审核
- 激活工具审核
- 激活码库存巡检
- 订单管理
- 支付记录管理
- 发货记录管理
- 异常任务处理
- 系统配置管理
- 操作日志与审计日志查看

---

## 9. 非功能需求

## 9.1 安全性
- JWT 鉴权
- 密码加密存储
- 角色权限控制
- 敏感字段脱敏
- 接口参数校验
- 登录失败限制
- 支付回调验签
- 激活工具上传审查
- 激活工具执行隔离
- 审计日志记录

## 9.2 性能
- Redis 缓存热点商品
- Redis 支撑验证码与幂等控制
- RabbitMQ 异步处理执行任务
- 数据库索引优化
- 分页查询
- 限流与防刷控制

## 9.3 可维护性
- 模块边界清晰
- 统一异常处理
- 统一返回结构
- API 文档自动生成
- 容器化部署
- 配置文件环境隔离

## 9.4 可扩展性
- 商品与发货解耦
- 支持多种交付模式
- 支持后续加入真实支付
- 支持后续拆出工具执行服务

---

## 10. 技术选型

## 10.1 前端技术栈
前端只做 Web 端，建议采用：

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus
- ECharts（后台图表可选）
- SCSS

### 选型说明
- Vue 3 + Vite 更适合个人项目快速启动与长期维护
- TypeScript 提高页面与接口协作可维护性
- Element Plus 适合后台管理系统与中后台风格页面

---

## 10.2 后端技术栈
后端建议采用：

- Java 21
- Spring Boot 4.0.3
- Spring Security
- Spring Data JPA
- Hibernate
- Bean Validation
- OpenAPI 文档
- Lombok
- Maven

截至 2026-03-12，Spring Boot 官方文档的 Stable 列表包含 4.0.3；系统要求说明其最低需要 Java 17，因此使用 Java 21 作为本项目统一版本是兼容的。([docs.spring.io](https://docs.spring.io/spring-boot/documentation.html?utm_source=openai))

---

## 10.3 数据与中间件
- PostgreSQL 16
- Redis
- RabbitMQ

---

## 10.4 部署技术
- Docker
- Docker Compose
- Nginx
- Linux 服务器

---

## 11. 系统架构设计

## 11.1 架构风格
本项目采用：

**前后端分离 + 模块化单体后端 + 单服务器容器部署**

说明：
- 前端单独构建为 Web 项目
- 后端单体应用按业务模块拆分
- 不做微服务
- 使用 Docker Compose 统一编排部署

---

## 11.2 容器部署组成

最终部署容器：

- `actrade-nginx`
- `actrade-web`
- `actrade-api`
- `actrade-postgres`
- `actrade-redis`
- `actrade-rabbitmq`

说明：
- 前端单独打包成容器
- 后端单独打包成容器
- PostgreSQL、Redis、RabbitMQ 独立容器运行
- 所有容器统一部署在服务器上

---

## 11.3 部署拓扑

```text
Browser
   |
   v
Nginx Container
   |------> Web Container（Vue Web）
   |
   v
API Container（Spring Boot）
   |------> PostgreSQL 16 Container
   |------> Redis Container
   |------> RabbitMQ Container
```

---

## 11.4 后端模块划分

建议按以下模块组织后端：

- `auth`：认证授权模块
- `user`：用户模块
- `merchant`：商家模块
- `product`：商品模块
- `inventory`：激活码库存模块
- `tool`：激活工具模块
- `order`：订单模块
- `payment`：支付模块
- `delivery`：发货模块
- `audit`：审核模块
- `log`：日志审计模块
- `common`：公共模块

---

## 12. 发货策略设计

## 12.1 商品交付类型

商品必须明确一种交付类型：

- `CODE_STOCK`：激活码库存发货
- `TOOL_EXECUTION`：激活工具执行发货

---

## 12.2 发货总流程抽象

无论哪种商品，支付成功后统一进入发货中心：

1. 支付成功
2. 写入支付记录
3. 创建发货任务
4. RabbitMQ 异步处理发货
5. 根据商品类型进入不同处理器
6. 输出发货结果
7. 变更订单状态
8. 写入日志和审计记录

---

## 12.3 库存发货策略
处理逻辑：

1. 根据商品查找可用激活码
2. 使用数据库事务锁定记录
3. 将状态改为 `LOCKED`
4. 绑定订单
5. 发货成功后改为 `SOLD`
6. 写入发货记录与激活记录

### 并发处理要求
- 需要防止超卖
- 需要保证一条激活码只能卖一次
- 建议使用：
  - 数据库行级锁
  - Redis 幂等控制
  - MQ 异步补偿

---

## 12.4 工具执行发货策略
处理逻辑：

1. 创建执行任务
2. 读取对应工具版本
3. 准备执行输入参数
4. 创建临时工作目录
5. 执行工具
6. 解析输出结果
7. 写入激活结果
8. 完成发货

### 关键要求
- 有超时控制
- 有错误日志
- 有执行结果结构化输出
- 有失败重试策略
- 有人工介入能力

---

## 13. 激活工具包规范

为了后续开发方便，建议一开始就定义**工具包标准**，避免后面执行逻辑混乱。

## 13.1 上传格式
建议只允许上传 `.zip` 工具包。

## 13.2 工具包目录结构建议

```text
tool-package/
├── manifest.json
├── bin/
│   └── main.jar 或 main.exe 或 main.sh
├── resources/
│   └── ...
└── README.md
```

---

## 13.3 `manifest.json` 建议字段

```json
{
  "toolName": "example-activator",
  "version": "1.0.0",
  "runtimeType": "jar",
  "entrypoint": "bin/main.jar",
  "execCommand": "java -jar bin/main.jar --input ${INPUT_FILE} --output ${OUTPUT_FILE}",
  "timeoutSeconds": 30,
  "maxMemoryMb": 256,
  "outputFormat": "json",
  "charset": "UTF-8"
}
```

---

## 13.4 平台输入规范
平台执行工具前，生成 `input.json`，写入工作目录。

### `input.json` 示例

```json
{
  "orderNo": "A202603120001",
  "productId": "uuid",
  "productName": "XX软件专业版",
  "userId": "uuid",
  "quantity": 1,
  "ext": {
    "machineCode": "",
    "email": ""
  }
}
```

---

## 13.5 平台输出规范
工具执行结束后，必须输出 `output.json`。

### `output.json` 示例

```json
{
  "success": true,
  "message": "ok",
  "codes": [
    "XXXX-YYYY-ZZZZ-AAAA"
  ],
  "attachments": [],
  "extra": {
    "licenseType": "professional"
  }
}
```

### 输出要求
- `success`：是否成功
- `message`：提示信息
- `codes`：生成的激活码数组
- `attachments`：附带授权文件路径（后续可扩展）
- `extra`：附加结果

---

## 13.6 工具执行约束
平台需对工具执行施加以下限制：

- 限制最大执行时间
- 限制最大内存
- 限制最大输出大小
- 禁止任意网络访问（至少在设计层面保留）
- 每次执行独立工作目录
- 保留 stdout / stderr 日志
- 执行完成后清理临时目录

> 说明：  
> V1 阶段可先在后端容器内受控执行；  
> 但从安全角度，后续强烈建议拆出独立的 tool-runner 执行容器。

---

## 14. 数据库设计

> 说明：表名尽量避免使用数据库保留字，例如不用 `order`，统一使用 `orders`。

---

## 14.1 用户表 `app_users`

| 字段名 | 类型 | 说明 |
|---|---|---|
| user_id | UUID | 主键 |
| username | varchar(64) | 登录账号，唯一 |
| password_hash | varchar(255) | 密码哈希 |
| nickname | varchar(64) | 昵称 |
| email | varchar(128) | 邮箱 |
| phone | varchar(32) | 手机号 |
| role | varchar(32) | 角色 |
| status | varchar(32) | 用户状态 |
| last_login_at | timestamp | 最后登录时间 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

### 索引建议
- `uk_app_users_username`
- `uk_app_users_email`
- `uk_app_users_phone`

---

## 14.2 商家表 `merchant`

| 字段名 | 类型 | 说明 |
|---|---|---|
| merchant_id | UUID | 主键 |
| user_id | UUID | 关联用户 |
| merchant_name | varchar(128) | 商家名称 |
| merchant_type | varchar(32) | 个人/团队/企业 |
| contact_name | varchar(64) | 联系人 |
| contact_email | varchar(128) | 联系邮箱 |
| contact_phone | varchar(32) | 联系电话 |
| license_info | text | 资质/授权说明 |
| audit_status | varchar(32) | 审核状态 |
| audit_remark | text | 审核备注 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

---

## 14.3 商品分类表 `product_category`

| 字段名 | 类型 | 说明 |
|---|---|---|
| category_id | UUID | 主键 |
| parent_id | UUID | 父分类 |
| name | varchar(64) | 分类名称 |
| sort_no | int | 排序 |
| status | varchar(32) | 状态 |
| created_at | timestamp | 创建时间 |

---


## 14.4 商品表 `product`

| 字段名 | 类型 | 说明 |
|---|---|---|
| product_id | UUID | 主键 |
| merchant_id | UUID | 商家 ID |
| category_id | UUID | 分类 ID |
| name | varchar(128) | 商品名称 |
| subtitle | varchar(255) | 副标题 |
| description | text | 简介 |
| cover_image | varchar(255) | 封面图 |
| delivery_mode | varchar(32) | `CODE_STOCK` / `TOOL_EXECUTION` |
| price | numeric(12,2) | 售价 |
| original_price | numeric(12,2) | 原价 |
| status | varchar(32) | 草稿/待审核/上架/下架 |
| stock_count | int | 可用库存快照 |
| sales_count | int | 销量 |
| version_no | int | 乐观锁版本号 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

### 说明
- `stock_count` 对库存模式有效
- 工具模式下可以作为展示字段，实际不依赖该值扣减

---

## 14.5 商品详情表 `product_detail`

| 字段名 | 类型 | 说明 |
|---|---|---|
| product_id | UUID | 主键 |
| detail_markdown | text | 商品详情 Markdown |
| usage_guide | text | 使用说明 |
| activation_notice | text | 激活注意事项 |
| refund_policy | text | 售后说明 |
| faq_content | text | FAQ |
| updated_at | timestamp | 更新时间 |

---
> Note:后面进入正式业务阶段，可以再拆成：

>    商家侧接口
>    管理后台接口
>    审核接口
>    上下架接口
>    商品详情接口
>    分类树接口


## 14.6 激活工具主表 `activation_tool`

| 字段名 | 类型 | 说明 |
|---|---|---|
| tool_id | UUID | 主键 |
| merchant_id | UUID | 商家 ID |
| product_id | UUID | 商品 ID |
| tool_name | varchar(128) | 工具名称 |
| current_version_id | UUID | 当前生效版本 |
| tool_status | varchar(32) | 待审核/启用/停用/拒绝 |
| audit_status | varchar(32) | 审核状态 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

---

## 14.7 激活工具版本表 `activation_tool_version`

| 字段名 | 类型 | 说明 |
|---|---|---|
| tool_version_id | UUID | 主键 |
| tool_id | UUID | 工具 ID |
| version_name | varchar(64) | 版本号 |
| file_id | UUID | 上传文件 ID |
| manifest_content | jsonb | 解析后的 manifest |
| runtime_type | varchar(32) | jar/exe/script |
| entrypoint | varchar(255) | 入口文件 |
| exec_command | text | 执行命令模板 |
| timeout_seconds | int | 超时时间 |
| max_memory_mb | int | 最大内存 |
| checksum | varchar(128) | 校验值 |
| status | varchar(32) | 草稿/待审核/启用/停用 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

---

## 14.8 激活码库存表 `activation_code_inventory`

| 字段名 | 类型 | 说明 |
|---|---|---|
| code_id | UUID | 主键 |
| product_id | UUID | 商品 ID |
| merchant_id | UUID | 商家 ID |
| batch_no | varchar(64) | 批次号 |
| code_value_encrypted | text | 加密后的激活码 |
| code_value_masked | varchar(128) | 脱敏显示值 |
| status | varchar(32) | 可用/锁定/已售/作废 |
| assigned_order_id | UUID | 关联订单 |
| assigned_order_item_id | UUID | 关联订单项 |
| expired_at | timestamp | 过期时间 |
| remark | varchar(255) | 备注 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

### 索引建议
- `idx_code_inventory_product_status`
- `idx_code_inventory_batch_no`
- `idx_code_inventory_assigned_order_id`

---

## 14.9 订单表 `orders`

| 字段名 | 类型 | 说明 |
|---|---|---|
| order_id | UUID | 主键 |
| order_no | varchar(64) | 订单号，唯一 |
| user_id | UUID | 用户 ID |
| merchant_id | UUID | 商家 ID |
| total_amount | numeric(12,2) | 总金额 |
| pay_amount | numeric(12,2) | 实付金额 |
| order_status | varchar(32) | 订单状态 |
| payment_status | varchar(32) | 支付状态 |
| payment_method | varchar(32) | 支付方式 |
| paid_at | timestamp | 支付时间 |
| remark | varchar(255) | 备注 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

### 索引建议
- `uk_orders_order_no`
- `idx_orders_user_id`
- `idx_orders_merchant_id`
- `idx_orders_created_at`

---

## 14.10 订单项表 `order_item`

| 字段名 | 类型 | 说明 |
|---|---|---|
| order_item_id | UUID | 主键 |
| order_id | UUID | 订单 ID |
| product_id | UUID | 商品 ID |
| product_name | varchar(128) | 下单时商品名快照 |
| delivery_mode | varchar(32) | 发货类型快照 |
| unit_price | numeric(12,2) | 单价 |
| quantity | int | 数量 |
| subtotal_amount | numeric(12,2) | 小计 |
| created_at | timestamp | 创建时间 |

---

## 14.11 支付记录表 `payment_record`

| 字段名 | 类型 | 说明 |
|---|---|---|
| payment_id | UUID | 主键 |
| order_id | UUID | 订单 ID |
| payment_method | varchar(32) | 支付方式 |
| payment_status | varchar(32) | 支付状态 |
| transaction_no | varchar(128) | 支付流水号 |
| callback_payload | text | 回调原文 |
| paid_at | timestamp | 支付时间 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

---

## 14.12 发货记录表 `delivery_record`

| 字段名 | 类型 | 说明 |
|---|---|---|
| delivery_id | UUID | 主键 |
| order_id | UUID | 订单 ID |
| order_item_id | UUID | 订单项 ID |
| product_id | UUID | 商品 ID |
| delivery_mode | varchar(32) | 发货模式 |
| delivery_status | varchar(32) | 待处理/处理中/成功/失败 |
| delivery_message | text | 发货信息 |
| delivered_at | timestamp | 发货完成时间 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

---

## 14.13 激活工具执行任务表 `tool_execution_task`

| 字段名 | 类型 | 说明 |
|---|---|---|
| task_id | UUID | 主键 |
| order_id | UUID | 订单 ID |
| order_item_id | UUID | 订单项 ID |
| product_id | UUID | 商品 ID |
| tool_version_id | UUID | 工具版本 ID |
| task_status | varchar(32) | 待执行/执行中/成功/失败 |
| retry_count | int | 重试次数 |
| max_retry_count | int | 最大重试次数 |
| work_dir | varchar(255) | 工作目录 |
| input_file_path | varchar(255) | 输入文件路径 |
| output_file_path | varchar(255) | 输出文件路径 |
| started_at | timestamp | 开始时间 |
| finished_at | timestamp | 结束时间 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

---

## 14.14 激活记录表 `activation_record`

| 字段名 | 类型 | 说明 |
|---|---|---|
| record_id | UUID | 主键 |
| order_id | UUID | 订单 ID |
| order_item_id | UUID | 订单项 ID |
| product_id | UUID | 商品 ID |
| user_id | UUID | 用户 ID |
| source_type | varchar(32) | 库存/工具 |
| activation_result | varchar(32) | 成功/失败 |
| code_masked | varchar(255) | 脱敏后的激活码 |
| raw_result_snapshot | jsonb | 执行结果快照 |
| response_message | text | 返回消息 |
| created_at | timestamp | 创建时间 |

---

## 14.15 上传文件表 `upload_file`

| 字段名 | 类型 | 说明 |
|---|---|---|
| file_id | UUID | 主键 |
| uploader_id | UUID | 上传人 |
| business_type | varchar(32) | 商品图/工具包/资质文件 |
| original_name | varchar(255) | 原始文件名 |
| storage_path | varchar(255) | 存储路径 |
| file_size | bigint | 文件大小 |
| mime_type | varchar(128) | 文件类型 |
| checksum | varchar(128) | 校验值 |
| created_at | timestamp | 创建时间 |

---

## 14.16 审核记录表 `audit_record`

| 字段名 | 类型 | 说明 |
|---|---|---|
| audit_id | UUID | 主键 |
| target_type | varchar(32) | 商家/商品/工具 |
| target_id | UUID | 对象 ID |
| audit_status | varchar(32) | 待审核/通过/拒绝 |
| auditor_id | UUID | 审核人 |
| audit_comment | text | 审核意见 |
| created_at | timestamp | 创建时间 |

---

## 14.17 操作日志表 `operation_log`

| 字段名 | 类型 | 说明 |
|---|---|---|
| log_id | UUID | 主键 |
| operator_id | UUID | 操作人 |
| module_name | varchar(64) | 模块名 |
| operation_type | varchar(64) | 操作类型 |
| request_uri | varchar(255) | 请求路径 |
| request_method | varchar(16) | 请求方法 |
| operation_result | varchar(32) | 成功/失败 |
| ip_address | varchar(64) | IP |
| created_at | timestamp | 创建时间 |

---

## 15. 枚举设计建议

## 15.1 用户状态
- `NORMAL`
- `DISABLED`
- `DELETED`

## 15.2 商家审核状态
- `PENDING`
- `APPROVED`
- `REJECTED`

## 15.3 商品状态
- `DRAFT`
- `PENDING_REVIEW`
- `ONLINE`
- `OFFLINE`

## 15.4 商品交付模式
- `CODE_STOCK`
- `TOOL_EXECUTION`

## 15.5 激活码库存状态
- `AVAILABLE`
- `LOCKED`
- `SOLD`
- `INVALID`

## 15.6 订单状态
- `PENDING_PAYMENT`
- `PAID`
- `DELIVERING`
- `COMPLETED`
- `CANCELLED`
- `CLOSED`

## 15.7 支付状态
- `UNPAID`
- `PAYING`
- `PAID`
- `FAILED`
- `CLOSED`

## 15.8 发货状态
- `PENDING`
- `PROCESSING`
- `SUCCESS`
- `FAILED`

## 15.9 工具状态
- `DRAFT`
- `PENDING_REVIEW`
- `ENABLED`
- `DISABLED`
- `REJECTED`

## 15.10 工具任务状态
- `PENDING`
- `RUNNING`
- `SUCCESS`
- `FAILED`

---

## 16. API 设计规划 (设想，具体情况具体决定)

## 16.1 设计原则
- RESTful 风格
- 统一路径前缀：`/api/v1`
- 统一返回结构
- JWT 鉴权
- 分页参数统一
- 下单、支付、发货需支持幂等

### 返回结构示例

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

---

## 16.2 认证接口
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/refresh-token`
- `POST /api/v1/auth/forgot-password`

---

## 16.3 用户接口
- `GET /api/v1/users/me`
- `PUT /api/v1/users/me`
- `PUT /api/v1/users/me/password`

---

## 16.4 商品接口
- `GET /api/v1/products`
- `GET /api/v1/products/{id}`
- `GET /api/v1/categories`

---

## 16.5 订单接口
- `POST /api/v1/orders`
- `GET /api/v1/orders/{id}`
- `GET /api/v1/orders/my`
- `POST /api/v1/orders/{id}/cancel`
- `GET /api/v1/orders/{id}/delivery`
- `GET /api/v1/orders/{id}/activation`

---

## 16.6 支付接口
> MVP 阶段建议先做模拟支付，保证主业务链路通。

- `POST /api/v1/payments/orders/{id}/mock-pay`
- `GET /api/v1/payments/orders/{id}/status`

后续再扩展真实支付：
- 支付宝回调
- 微信支付回调

---

## 16.7 商家接口
- `POST /api/v1/merchant/apply`
- `GET /api/v1/merchant/profile`
- `POST /api/v1/merchant/products`
- `PUT /api/v1/merchant/products/{id}`
- `POST /api/v1/merchant/products/{id}/submit-review`

---

## 16.8 激活码库存接口
- `POST /api/v1/merchant/products/{id}/codes/upload`
- `GET /api/v1/merchant/products/{id}/codes`
- `POST /api/v1/merchant/products/{id}/codes/invalidate`

---

## 16.9 激活工具接口
- `POST /api/v1/merchant/products/{id}/tools/upload`
- `GET /api/v1/merchant/products/{id}/tools`
- `GET /api/v1/merchant/tools/{toolId}/versions`
- `POST /api/v1/merchant/tools/{toolId}/enable-version`
- `GET /api/v1/merchant/tools/{toolId}/tasks`

---

## 16.10 管理后台接口
- `GET /api/v1/admin/users`
- `GET /api/v1/admin/merchants`
- `POST /api/v1/admin/merchants/{id}/approve`
- `POST /api/v1/admin/merchants/{id}/reject`
- `GET /api/v1/admin/products/review`
- `POST /api/v1/admin/products/{id}/approve`
- `POST /api/v1/admin/products/{id}/reject`
- `GET /api/v1/admin/tools/review`
- `POST /api/v1/admin/tools/{id}/approve`
- `POST /api/v1/admin/tools/{id}/reject`
- `POST /api/v1/admin/delivery/{id}/retry`

---

## 17. 前端页面规划

## 17.1 用户端页面
- 登录页
- 注册页
- 首页
- 商品列表页
- 商品详情页
- 下单页
- 支付页
- 支付成功页
- 我的订单页
- 订单详情页
- 个人中心页

---

## 17.2 商家端页面
- 商家入驻页
- 商家工作台
- 商品管理页
- 商品编辑页
- 库存激活码管理页
- 激活工具管理页
- 工具版本页
- 执行日志页
- 销售统计页

---

## 17.3 管理后台页面
- 管理首页
- 用户管理
- 商家审核
- 商品审核
- 工具审核
- 订单管理
- 发货管理
- 异常任务页
- 审计日志页
- 系统配置页

---

## 18. Redis 设计

Redis 主要用于：

- 登录验证码缓存
- 邮箱/短信验证码缓存
- JWT 黑名单（如实现）
- 下单防重 Token
- 热门商品缓存
- 商品详情缓存
- 幂等控制 Key
- 限流计数器
- 短期任务状态缓存

### Key 设计建议
- `auth:captcha:{uuid}`
- `auth:email-code:{email}`
- `order:submit-token:{userId}`
- `product:detail:{productId}`
- `product:hot:list`
- `delivery:idempotent:{orderNo}`

---

## 19. RabbitMQ 设计

RabbitMQ 主要用于：

- 支付成功后的异步发货任务
- 工具执行任务
- 发货失败重试
- 未支付订单超时关闭
- 邮件通知（后续）
- 审核结果通知（后续）

### 建议队列
- `q.order.paid`
- `q.delivery.dispatch`
- `q.tool.execute`
- `q.delivery.retry`
- `q.order.close.delay`

---

## 20. 安全设计

## 20.1 账户安全
- BCrypt 密码加密
- JWT 鉴权
- 权限分级
- 登录失败次数限制
- 敏感操作校验

## 20.2 数据安全
- 激活码加密存储
- 返回前脱敏显示
- 上传文件校验
- SQL 注入防护
- XSS 基础过滤
- 审计日志留存

## 20.3 业务安全
- 防重复下单
- 防重复支付
- 支付回调幂等
- 发货任务幂等
- 激活码库存并发锁定
- 异常订单监控

## 20.4 激活工具安全
这是本项目最需要重视的风险点。

### 最低要求
- 只允许白名单文件类型
- 仅允许 zip 包上传
- 上传后生成 checksum
- 解压目录隔离
- 执行目录隔离
- 限制执行超时
- 限制内存
- 记录 stdout/stderr
- 失败时保留结果快照
- 可人工停用该工具版本

### 后续增强建议
- 独立 tool-runner 容器
- 容器级资源限制
- seccomp / apparmor / cgroup 控制
- 网络隔离
- 杀毒扫描
- 沙箱执行

---

## 21. 容器化与部署方案

## 21.1 最终部署目标
本项目最终采用：

**单服务器 + Docker Compose + Nginx 反向代理**

---

## 21.2 容器列表

| 容器名 | 作用 |
|---|---|
| actrade-nginx | 反向代理、静态资源入口 |
| actrade-web | 前端 Web 容器 |
| actrade-api | Spring Boot API 容器 |
| actrade-postgres | PostgreSQL 16 |
| actrade-redis | Redis |
| actrade-rabbitmq | RabbitMQ |

---

## 21.3 数据持久化目录建议

```text
/data/actrade/
├── postgres/
├── redis/
├── rabbitmq/
├── uploads/
│   ├── product/
│   ├── tools/
│   └── merchant/
├── runtime/
│   ├── tool-workdir/
│   └── logs/
└── nginx/
    └── logs/
```

---

## 21.4 Nginx 职责
- 对外统一入口
- 反向代理 `/api`
- 托管前端静态资源
- 配置 HTTPS（后续）
- 记录访问日志

---

## 21.5 Web 容器职责
- 运行前端构建产物
- 提供静态页面资源

> 实际实现时，也可以将前端静态资源直接交给 Nginx 托管。  
> 但如果你已明确“前端单独打包成容器”，则保留 `actrade-web` 容器。

---

## 21.6 API 容器职责
- 提供业务 API
- 连接 PostgreSQL / Redis / RabbitMQ
- 处理订单、支付、发货、工具执行、审核等核心逻辑

---

## 21.7 Docker Compose 编排重点
- 所有服务加入同一 bridge 网络
- PostgreSQL / Redis / RabbitMQ 挂载卷
- API 容器挂载 uploads 与 runtime 目录
- 环境变量使用 `.env` 管理
- 区分 dev / prod 配置

---

## 22. 项目目录规划

```text
actrade/
├── README.md
├── docs/
│   ├── architecture.md
│   ├── business-flow.md
│   ├── api-design.md
│   ├── database-design.md
│   ├── tool-package-spec.md
│   └── deployment.md
├── actrade-web/
│   ├── src/
│   │   ├── api/
│   │   ├── assets/
│   │   ├── components/
│   │   ├── layouts/
│   │   ├── router/
│   │   ├── stores/
│   │   ├── views/
│   │   ├── utils/
│   │   └── App.vue
│   ├── public/
│   ├── Dockerfile
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
├── actrade-api/
│   ├── src/main/java/
│   │   └── com/actrade/
│   │       ├── auth/
│   │       ├── user/
│   │       ├── merchant/
│   │       ├── product/
│   │       ├── inventory/
│   │       ├── tool/
│   │       ├── order/
│   │       ├── payment/
│   │       ├── delivery/
│   │       ├── audit/
│   │       ├── log/
│   │       └── common/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/
│   ├── Dockerfile
│   └── pom.xml
├── nginx/
│   ├── conf.d/
│   └── Dockerfile
├── deploy/
│   ├── docker-compose.yml
│   ├── .env
│   └── init/
└── scripts/
    ├── build.sh
    ├── deploy.sh
    └── backup.sh
```

---

## 23. 环境变量规划

建议统一使用 `.env` 管理：

```env
APP_NAME=actrade
APP_PORT=8080

POSTGRES_DB=actrade
POSTGRES_USER=actrade
POSTGRES_PASSWORD=change_me

REDIS_HOST=actrade-redis
REDIS_PORT=6379

RABBITMQ_HOST=actrade-rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=actrade
RABBITMQ_PASSWORD=change_me

JWT_SECRET=change_me_please
JWT_EXPIRE_SECONDS=7200

UPLOAD_BASE_PATH=/data/actrade/uploads
TOOL_BASE_PATH=/data/actrade/uploads/tools
TOOL_WORKDIR_BASE_PATH=/data/actrade/runtime/tool-workdir

PAYMENT_MODE=mock
```

---

## 24. 开发阶段规划

## 24.1 第一阶段：需求收敛与基础设计
目标：
- 明确业务边界
- 明确交付模式
- 完成数据库设计
- 完成接口清单
- 完成页面清单
- 搭建前后端骨架

交付物：
- README
- API 清单
- ER 图
- 页面原型草图
- 项目骨架代码

---

## 24.2 第二阶段：MVP 用户交易链路
目标：
- 注册/登录
- 商品浏览
- 商品详情
- 创建订单
- 模拟支付
- 我的订单
- 订单详情
- 激活码库存发货

交付物：
- 用户端可用
- 库存模式完整打通
- 基础管理后台可查看订单

---

## 24.3 第三阶段：商家中心
目标：
- 商家入驻
- 商品创建
- 商品编辑
- 激活码上传
- 商品提审
- 管理员审核上架

交付物：
- 商家端基本可用
- 商品审核闭环完成

---

## 24.4 第四阶段：激活工具托管
目标：
- 工具包上传
- manifest 解析
- 工具审核
- 订单支付后触发工具执行
- 记录执行结果
- 失败重试

交付物：
- 工具执行链路完成
- 工具任务日志可查

---

## 24.5 第五阶段：性能与稳定性优化
目标：
- Redis 缓存
- RabbitMQ 发货异步化
- 超时订单关闭
- 库存并发控制优化
- 日志与审计增强

---

## 24.6 第六阶段：容器化部署
目标：
- 编写前端 Dockerfile
- 编写后端 Dockerfile
- 编写 Nginx 配置
- 编写 docker-compose.yml
- 服务器部署验证

交付物：
- 单服务器完整部署方案
- 一键启动脚本
- 生产配置说明

---

## 25. MVP 范围建议

## 25.1 必做
- 用户注册/登录
- 商品列表/详情
- 下单
- 模拟支付
- 激活码库存上传
- 支付成功后库存发货
- 订单中心
- 商家创建商品
- 管理员审核商品
- Docker Compose 部署

## 25.2 第二优先级
- 激活工具上传
- 工具审核
- 工具执行发货
- 执行日志查看
- 发货失败重试

## 25.3 后续迭代
- 真实支付宝/微信支付
- 优惠券
- 商品评价
- 统计报表增强
- 更严格的工具执行隔离
- Prometheus / Grafana 监控

---

## 26. 测试策略

## 26.1 单元测试
- 订单状态流转测试
- 库存扣减测试
- 工具执行结果解析测试
- 权限校验测试

## 26.2 集成测试
- 下单到发货流程测试
- 支付成功回调测试
- MQ 消费测试
- Redis 幂等测试

## 26.3 前端测试
- 页面路由测试
- 表单校验测试
- 登录状态测试
- 关键页面联调测试

## 26.4 人工验收测试
- 库存模式发货
- 工具模式发货
- 后台审核
- 异常任务处理
- Docker 部署验证

---

## 27. 风险点与解决思路

## 27.1 激活工具执行风险
### 风险
- 上传恶意文件
- 任意命令执行
- 资源耗尽
- 输出异常导致发货失败

### 对策
- zip 白名单
- manifest 校验
- 路径隔离
- 超时限制
- 内存限制
- 人工审核
- 日志审计
- 后续拆独立执行容器

---

## 27.2 激活码库存并发风险
### 风险
- 超卖
- 重复发货
- 多线程竞争

### 对策
- 数据库事务
- 行级锁
- Redis 幂等
- 发货状态机

---

## 27.3 支付幂等风险
### 风险
- 重复回调
- 状态错乱
- 重复发货

### 对策
- 订单状态机
- 支付记录唯一约束
- 发货幂等 Key
- 重复消息拦截

---

## 27.4 文件存储风险
### 风险
- 文件丢失
- 文件损坏
- 路径混乱

### 对策
- 上传文件统一入库
- 文件校验值
- 挂载宿主机持久卷
- 定期备份

---

## 28. 验收标准

项目达到以下标准可视为第一版合格：

1. 用户可注册、登录、浏览商品、下单、支付
2. 库存型商品可自动发货
3. 用户可在订单详情查看激活码
4. 商家可创建商品并上传库存激活码
5. 管理员可审核商家与商品
6. 激活工具可上传、审核并执行一次完整流程
7. Redis 和 RabbitMQ 已进入实际链路
8. 前后端均容器化
9. PostgreSQL、Redis、RabbitMQ 均以容器运行
10. 可在单台服务器通过 Docker Compose 启动运行

---

## 29. 当前最终技术决策

### 前端
- **Web 开发**
- Vue 3 + TypeScript + Vite + Element Plus

### 后端
- **Java 21**
- **Spring Boot 4.0.3**
- Spring Security
- Spring Data JPA
- Maven

截至 2026-03-12，Spring Boot 官方文档将 4.0.3 列为 Stable，系统要求页说明其要求至少 Java 17，因此与 Java 21 兼容。([docs.spring.io](https://docs.spring.io/spring-boot/documentation.html?utm_source=openai))

### 数据与中间件
- **PostgreSQL 16**
- Redis
- RabbitMQ

### 激活业务模式
- **上传激活工具**
- **上传激活码作为库存发货**
- **不做 API 调用**

### 部署方式
- 前端容器
- 后端容器
- PostgreSQL 容器
- Redis 容器
- RabbitMQ 容器
- Nginx 容器
- Docker Compose 统一编排

---

