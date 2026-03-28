# Activation-Runner 微服务独立文档

## 1. 文档说明

本文档用于说明 `activation-runner` 微服务的职责定位、技术选型、部署方式、执行沙箱方案、二进制文件存储方案、与 `actrade-api` 的协作方式，以及现阶段数据库规划。

`activation-runner` 是围绕“激活工具执行”能力拆分出的独立执行服务，其核心目标是：

1. **在隔离沙箱中安全执行商家上传的激活工具**
2. **根据工具执行结果生成激活码或其他履约产物**
3. **管理执行过程中的工具包拉取、校验、日志采集、结果回传**

---

## 2. 微服务定位

### 2.1 服务职责

`activation-runner` 主要负责以下能力：

- 消费由 `actrade-api` 投递的激活执行任务
- 拉取商家已上传的激活工具二进制文件
- 解析并校验工具包结构与 Manifest
- 创建隔离沙箱执行环境
- 执行商家上传的激活工具
- 收集执行日志、退出码、结果文件
- 将执行结果回传至 `actrade-api`
- 执行完成后清理临时目录与沙箱资源
---

## 3. 技术选型

## 3.1 基础框架

- **Spring Boot**：与 `actrade-api` 保持一致
- **Java**：21
- **构建工具**：Maven

为降低团队维护成本，`activation-runner` 与 `actrade-api` 采用一致的 Java / Spring 技术栈，便于统一代码规范、依赖管理和部署方式。

---

## 3.2 消息队列

使用当前已在运行的 RabbitMQ 容器，作为 `actrade-api` 与 `activation-runner` 之间的任务传输与调度通道。

### 当前 MQ 配置

```text
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5673
RABBITMQ_MANAGEMENT_PORT=15673
RABBITMQ_USER=actrade
RABBITMQ_PASSWORD=actrade123
RABBITMQ_VHOST=/
```

### MQ 主要用途

- `actrade-api` 向 `activation-runner` 投递执行任务
- `activation-runner` 作为消费者拉取任务
- 后续支持失败重试、死信队列、延迟重试、任务补偿

---

## 3.3 沙箱执行方案

### 现阶段方案

- **Rootless Docker**
- **gVisor**

> **Rootless Docker 与 gVisor 不应视为“docker-compose 中再起一个普通容器即可”的业务依赖。**  
> 它们应安装在 `activation-runner` 所在宿主机上，作为该服务执行隔离容器时所依赖的底层运行时能力。

### 设计原因

`activation-runner` 的核心问题是：

- 执行第三方上传的二进制内容
- 防止恶意代码影响主业务服务
- 限制资源消耗
- 避免直接在宿主机进程内执行不可信程序

执行环境必须具备：

- 非 root 运行
- 隔离文件系统
- 限制 CPU / 内存 / 执行时长
- 默认最小权限
- 任务级容器销毁能力

---

## 3.4 二进制持久化方案

### 现阶段方案

- **MinIO 兼容对象存储**

### 存储原则

- **数据库只保存文件元数据**
- **对象存储保存文件本体**
- `activation-runner` 仅按需下载、临时缓存、执行后清理
- 不将二进制文件长期保存在 Runner 本地磁盘
- 不将二进制文件直接存入 PostgreSQL

---

## 4. 微服务工作模式

## 4.1 总体模式

`activation-runner` 采用“**异步任务消费 + 沙箱执行 + 回调主服务**”的模式。

执行链路如下：

1. `actrade-api` 创建激活执行任务
2. `actrade-api` 将任务消息投递至 RabbitMQ
3. `activation-runner` 消费任务消息
4. `activation-runner` 查询任务详情与工具元数据
5. `activation-runner` 从对象存储拉取工具包
6. `activation-runner` 创建隔离容器并执行工具
7. 收集执行日志、结果文件、退出状态
8. 调用 `actrade-api` 内部接口回传执行结果
9. `actrade-api` 更新任务状态、库存、履约信息

---

## 4.2 工具执行方式

- **每个任务一个隔离容器**
- 每个任务执行完成后立即销毁容器
- 不允许多个商家任务长期共用同一个执行容器

### 设计目的

- 降低跨任务污染风险
- 提高执行隔离性
- 便于控制任务级资源上限
- 便于后续做重试、超时处理与审计定位

---

## 4.3 结果回传方式

执行结果由 `activation-runner` 调用 `actrade-api` 内部接口进行回传，不直接写主业务数据库。

### 原则

- `activation-runner`：负责执行与结果采集
- `actrade-api`：负责主数据落库与状态推进

这样设计的好处：

- 业务边界更清晰
- 主数据统一归口管理
- 后续拆库或拆服务更容易
- 避免执行服务直接持有主库写权限

---

## 5. 部署边界规划

## 5.1 推荐部署边界

### 宿主机安装
以下组件建议安装在 `activation-runner` 宿主机：

- Rootless Docker
- gVisor

### 容器部署
以下组件可以通过 Docker / Docker Compose 部署：

- PostgreSQL
- RabbitMQ
- MinIO
- actrade-api
- activation-runner

---

## 5.2 当前推荐部署方式

建议将 `activation-runner` 部署在具备以下条件的专用节点或专用宿主机上：

- 已安装 Rootless Docker
- 已安装 gVisor runtime
- 可访问 RabbitMQ
- 可访问 MinIO
- 可访问 `actrade-api` 内部接口
- 具备足够的本地临时磁盘空间用于任务执行缓存

---

## 6. 二进制文件管理方案

## 6.1 文件持久化位置

激活工具文件本体统一存入对象存储，不存入数据库，不长期落盘在 Runner 本地。

### 具体职责划分

#### `actrade-api`
- 管理文件元数据 `file_asset`
- 生成上传凭证或登记上传结果
- 管理工具版本与文件关联关系

#### MinIO
- 持久化存储工具包、日志归档、执行结果文件

#### `activation-runner`
- 仅按需拉取工具包到本地临时目录
- 执行完成后清理本地缓存

---

## 6.2 本地缓存策略

Runner 本地磁盘只作为临时工作空间使用，包括：

- 工具包下载缓存
- 工具包解压目录
- 任务工作目录
- 输出结果暂存目录
- 日志临时文件

### 原则

- 临时文件必须设置清理机制
- 任务完成后立即删除
- 异常中断任务由后台清理器定期回收
- 不将 Runner 本地磁盘作为长期文件存储

---

## 7. 数据库规划

## 7.1 新增表设计

现阶段新增以下三张表：

- `activation_task`
- `activation_task_attempt`
- `activation_task_artifact`

### 表用途说明

#### `activation_task`
记录激活执行任务主信息，如任务状态、任务来源、关联订单、关联工具版本等。

#### `activation_task_attempt`
记录单个任务的每次执行尝试，用于重试追踪、失败分析、执行节点定位。

#### `activation_task_artifact`
记录任务执行产物与日志文件关联关系，如 stdout、stderr、result.json、output.zip 等。


---

## 7.2 主数据写入原则

三张表并入 `actrade` 数据库，当前设计原则是：

> **`activation-runner` 不直接写主库。**

### 推荐方式

由 `actrade-api` 负责：

- 创建任务记录
- 更新任务状态
- 写入任务尝试记录
- 写入任务产物记录
- 更新激活码库存
- 更新订单履约状态

`activation-runner` 通过：

- RabbitMQ 收任务
- 内部 HTTP 接口回传结果

与 `actrade-api` 协作。

---

## 8. 执行安全设计

## 8.1 基本原则

`activation-runner` 执行的是商家上传的二进制激活工具，因此必须按“不可信代码执行”标准进行设计。

必须遵守以下原则：

- 不在 `actrade-api` 进程内执行工具
- 不在宿主机直接裸执行商家上传文件
- 不以 root 权限运行执行容器
- 不允许访问宿主机敏感目录
- 不允许长期保留执行环境
- 不允许默认开放外部网络访问

---

## 8.2 执行容器约束

每个任务的执行容器至少应满足：

- 非 root 运行
- 禁止特权模式
- 禁止权限提升
- 限制 CPU、内存、超时时间
- 使用只读工具目录
- 使用独立临时工作目录
- 执行完成后销毁容器

---

## 8.3 后续安全增强方向

后续可以逐步增加：

- 文件扫描与黑白名单机制
- 工具包结构审计
- 执行日志归档
- 商家级配额限制
- 审批流与人工复核
- 多租户隔离增强

---

## 9. 后续开发计划

本阶段需要为后续演进预留空间，但不要求一次性全部实现。

## 9.1 下一阶段演进方向

- 切换为：**Kubernetes Job + gVisor RuntimeClass**
- 增加日志归档
- 增加执行审计
- 增加执行产物回放能力
- 增加自动扩缩容
- 增加任务重试、死信队列、补偿机制
- 增加任务监控面板与告警能力

---

## 9.2 当前阶段优先目标

当前阶段优先落地以下能力：

1. `activation-runner` 独立工程初始化
2. RabbitMQ 任务消费链路打通
3. MinIO 文件拉取链路打通
4. 工具包下载、校验、解压流程实现
5. 单任务单容器执行流程实现
6. 执行结果回调 `actrade-api`
7. 基础日志、异常处理、超时处理实现

---

## 10. 当前阶段推荐结论

### 10.1 组件部署结论

- **MinIO：可以通过容器启动运行**
- **Rootless Docker：应安装在 Runner 宿主机**
- **gVisor：应安装在 Runner 宿主机**

### 10.2 数据库

- 三张任务相关表：
  - `activation_task`
  - `activation_task_attempt`
  - `activation_task_artifact`
- **现阶段推荐并入 `actrade` 数据库**
- **由 `actrade-api` 负责主数据写入与状态流转**
- **`activation-runner` 不直接写主业务数据库**

---

## 11. 当前规划摘要

`activation-runner` 是 ACTrade 中负责“激活工具执行”的独立执行服务。
其定位不是主业务服务，而是围绕不可信二进制执行场景构建的安全执行平面。

当前阶段方案如下：

- 技术栈：Spring Boot + Java 21
- 消息队列：RabbitMQ
- 沙箱：Rootless Docker + gVisor
- 二进制持久化：MinIO
- 执行方式：每任务一个隔离容器
- 结果处理：回调 `actrade-api`
- 数据库策略：三张任务表并入 `actrade` 数据库，但不由 Runner 直接写库


