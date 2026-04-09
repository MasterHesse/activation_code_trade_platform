# ACTrade 项目开发计划文档

> 更新日期：2026-04-09
> 当前项目状态：✅ 支付流程已修复，自动发货功能待修复
> - **M1 核心业务与基础设施**：✅ 已完成
> - **M2 Runner健康检查与可观测性**：✅ 已完成 (2026-04-07)
> - **M3 Vue3前端工程**：✅ 已完成 (2026-04-07)
> - **JWT 认证授权**：✅ 已完成 (2026-04-07)
> - **M4 支付流程修复**：✅ 已完成 (2026-04-09)
> - **M5 自动发货功能**：🔄 进行中 (2026-04-09)

---

## 一、项目整体进度概览

### 1.1 已完成模块

| 模块 | 状态 | 说明 |
|------|------|------|
| actrade-api 核心业务 | ✅ 95% | 订单、支付、商品、用户、激活码库存管理 |
| activation-runner 骨架 | ✅ 完成 | 框架搭建完成，实际执行已集成 |
| **JWT 认证授权** | ✅ **已完成** | **JWT Token 生成/解析、登录/注册/刷新/登出、内部接口认证** |
| **M2 Runner健康检查与可观测性** | ✅ **已完成** | **Prometheus指标、Docker/MinIO/RabbitMQ健康检查、Micrometer集成** |
| **M3 Vue3前端工程** | ✅ **已完成** | **Vite+TypeScript+Pinia+Vue Router+Element Plus完整前端项目** |
| **M4 支付流程修复** | ✅ **已完成** | **支付宝沙箱集成、formHtml传递、sessionStorage存储、支付状态同步** |
| 基础设施 | ✅ 95% | Docker Compose、数据库设计、中间件配置、Internal Token |
| 文档 | ✅ 完成 | 项目计划、结构设计、activation-runner 独立文档 |

### 1.2 未完成模块

| 模块 | 状态 | 优先级 |
|------|------|--------|
| activation-runner 产物上传 | ✅ 已完成 | P1 |
| Runner 健康检查增强 | ✅ 已完成 | P1 |
| 支付流程修复 | ✅ 已完成 | P1 |
| **自动发货功能** | 🔄 **进行中** | **P1 - 支付成功后自动发货逻辑待修复** |
| 管理后台接口 | 🟡 部分缺失 | P2 |
| 测试代码 | 🟡 部分完成 | P2 |
| 前端工程 | ✅ 已完成 | P2 |

---

## 二、activation-runner 模块详细分析

### 2.1 当前状态总结

#### ✅ 已完成部分

| 组件 | 文件 | 状态 |
|------|------|------|
| 项目骨架 | pom.xml, ActivationRunnerApplication.java | ✅ |
| 消息监听 | ActivationTaskMessageListener.java | ✅ |
| 任务编排 | ActivationExecutionOrchestrator.java/Impl.java | ✅ (已集成 MinIO 下载) |
| 工作空间管理 | WorkspaceManager.java | ✅ |
| Actrade API 客户端 | ActradeInternalApiClient.java | ✅ |
| RabbitMQ 配置 | RabbitConfig.java | ✅ |
| MinIO 客户端配置 | MinioConfig.java, MinioProperties.java | ✅ |
| 工具包下载服务 | PackageDownloadService.java | ✅ |
| SHA-256 校验 | ChecksumValidator.java | ✅ |
| 压缩包解压 | ArchiveExtractor.java | ✅ |
| Docker 执行引擎 | DockerExecutionService.java, DockerCommandBuilder.java | ✅ 已完成 |
| 日志采集 | LogCollector.java | ✅ 已完成 |
| gVisor 沙箱配置 | GVisorProperties.java, DockerConfiguration.java | ✅ 已完成 |
| 容器执行结果 | ContainerExecutionResult.java, ContainerExecutionContext.java | ✅ 已完成 |
| 各属性配置 | RunnerProperties, DockerProperties 等 | ✅ |
| DTO 定义 | ClaimTaskRequest/Response, FinishTaskRequest/Response 等 | ✅ |
| 单元测试 | ChecksumValidatorTest.java, ArchiveExtractorTest.java, DockerCommandBuilderTest.java, DockerExecutionServiceTest.java, ArtifactUploadServiceTest.java | ✅ |
| 文档 | activation-runner文档.md | ✅ |
| **M2 可观测性** | | |
| Prometheus 指标 | RunnerMetrics.java, pom.xml micrometer-registry-prometheus | ✅ |
| Docker 健康检查 | DockerHealthIndicator.java | ✅ |
| MinIO 健康检查 | MinioHealthIndicator.java | ✅ |
| RabbitMQ 健康检查 | RabbitMQHealthIndicator.java | ✅ |
| Actuator 配置 | application.yaml Prometheus/健康检查分组 | ✅ |
| 指标埋点 | ActivationTaskMessageListener 集成 Micrometer | ✅ |

#### ❌ 待实现部分

| 功能 | 说明 | 工作量 | 状态 |
|------|------|--------|------|
| 工具包下载 | 从 MinIO 下载激活工具包 | 中 | ✅ 已完成 |
| 工具包校验 | SHA-256 校验 | 中 | ✅ 已完成 |
| 压缩包解压 | ZIP 解压 | 中 | ✅ 已完成 |
| Docker 容器执行 | 启动隔离容器运行工具 | 大 | ✅ 已完成 |
| 执行日志采集 | stdout/stderr 实时采集 | 中 | ✅ 已完成 |
| 执行结果上传 | 结果文件上传至 MinIO | 中 | ✅ 已完成 |
| gVisor 沙箱集成 | 安全运行时配置 | 大 | 🟡 基础完成 |
| 定时清理任务 | 过期工作空间清理 | 小 | 🟡 基础完成 |
| 健康检查增强 | Docker/MinIO/RabbitMQ 连接检查 | 小 | ✅ 已完成 |
| Prometheus 指标 | Micrometer 指标收集与导出 | 小 | ✅ 已完成 |

### 2.2 执行链路现状

```
当前完整执行流程：
┌─────────────────────────────────────────────────────────────────┐
│ MQ 消息入队 (activation.task.execute)                            │
└───────────────────────────────┬─────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│ ActivationTaskMessageListener.onMessage()                        │
│ - 接收消息 → ACK/NACK/REJECT 决策                               │
└───────────────────────────────┬─────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│ ActivationExecutionOrchestratorImpl.handle()                    │
│ - claimTask() 调用 actrade-api                                  │
│ - createWorkspace() 创建工作目录                                 │
│ - downloadPackage() 下载工具包 (MinIO)                          │
│ - extractPackage() 解压工具包                                    │
│ - executeInContainer() 容器执行 (gVisor 沙箱)                   │
│ - collectLogs() 采集日志                                        │
│ - uploadArtifacts() 上传执行产物 (MinIO) ✅ 新增                │
│ - finishTask() 回调 actrade-api                                 │
└───────────────────────────────┬─────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│ 工作目录结构已创建                                               │
│ .runner-workspace/                                              │
│ └── {taskNo}__attempt-{no}__{uuid}/                            │
│     ├── input/                                                  │
│     ├── output/  (含 result.json)                              │
│     ├── logs/    (含 stdout.log, stderr.log)                   │
│     └── temp/                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 已实现的关键能力

#### 2.3.1 MinIO 工具包下载 ✅
```java
// PackageDownloadService
- downloadPackage(PackageAssetDto asset) → Path localPackagePath
- verifyChecksum(Path file, String expectedSha256)
- extractPackage(Path archive, Path targetDir) → Path extractedDir
```

#### 2.3.2 Docker 容器执行 ✅
```java
// DockerExecutionService
- executeContainerTask(...) → ContainerExecutionResult

#### 2.3.3 产物上传服务 ✅ (2026-04-07 新完成)
```java
// ArtifactUploadService
- collectAndUploadArtifacts(workspace, taskNo, attemptNo) → ArtifactUploadResult
  - 上传日志文件: stdout.log, stderr.log
  - 上传结果文件: result.json
  - 上传 output 目录产物
- 上传产物自动计算 SHA-256 校验和
- ObjectKey 格式: {taskNo}/{attemptNo}/{artifactType}/{fileName}
- Bucket: activation-results (可通过配置自定义)
```

**产物类型说明**：
| 类型 | 来源 | 说明 |
|------|------|------|
| log-stdout | logs/stdout.log | 标准输出日志 |
| log-stderr | logs/stderr.log | 错误输出日志 |
| result-json | output/result.json | 结构化执行结果 |
| output | output/* | 容器执行产生的任意文件 |

**单元测试覆盖** (13 个测试用例全部通过)：
- 空 workspace 处理
- 日志文件上传
- result.json 上传
- output 目录文件上传
- SHA-256 校验和计算
- MIME 类型自动检测
- ObjectKey 格式验证
- null 值处理
- 结果工厂方法
  - checkImageExists() 检查镜像
  - pullImage() 拉取镜像
  - createContainer() 创建容器
  - startContainer() 启动容器
  - collectLogs() 采集日志
  - cleanupContainer() 清理容器

// DockerCommandBuilder
- buildPullCommand(image) 构建镜像拉取命令
- buildCreateCommand() 构建容器创建命令 (含 gVisor 配置)
- buildStartCommand() 构建容器启动命令
- buildRunCommand() 构建容器运行命令
- buildStopCommand() 构建容器停止命令
- buildRmCommand() 构建容器删除命令
```

#### 2.3.3 gVisor 沙箱集成 ✅
```java
// GVisorProperties - 29+ 配置项
- enabled: 启用 gVisor 沙箱
- runtimeName: runsc
- sandboxType: application/hostinet
- fileSystemType: overlay/passthrough
- networkMode: sandboxed/hostinet/none
- traceSyscall: 系统调用跟踪
- debug: 调试模式

// 通过 RUNSC_FLAG_* 环境变量传递配置
- RUNSC_FLAG_debug=true
- RUNSC_FLAG_trace=true
- RUNSC_FLAG_net=sandboxed
```

---

## 三、activation-runner 完善开发计划

### 阶段一：MinIO 集成与工具包下载 (2-3天) ✅ 已完成

#### Day 1: MinIO 客户端配置 ✅
- [x] 创建 MinioClient 配置类
- [x] 实现 PackageDownloadService
- [x] 实现 SHA-256 校验逻辑
- [x] 单元测试

#### Day 2: 工具包解压与 Manifest 解析 ✅
- [x] 实现 ZipArchiveExtractor
- [x] 创建 Manifest 模型类
- [x] 实现 ManifestParser
- [x] manifest.yaml 规范设计
- [x] 单元测试

#### Day 3: 集成到 Orchestrator ✅
- [x] 修改 ActivationExecutionOrchestratorImpl
- [x] 添加包下载步骤
- [x] 添加解压验证步骤
- [x] 端到端测试

### 阶段二：Docker 执行引擎 (3-5天) ✅ 已完成

#### Day 4-5: Docker 执行核心 ✅
- [x] 创建 DockerProperties 配置
- [x] 实现 DockerCommandBuilder
- [x] 实现 DockerExecutionService
- [x] 实现进程监控与日志采集
- [x] 超时与资源限制处理

#### Day 6: gVisor 沙箱集成 ✅
- [x] gVisor runtime 检测
- [x] 安全容器配置
- [x] 网络隔离验证
- [x] 权限限制验证

#### Day 7: 单元测试 ✅
- [x] DockerCommandBuilderTest (17+ 测试用例)
- [x] DockerExecutionServiceTest (20+ 测试用例)

### 阶段三：结果处理与回传 (1-2天) ✅ 已完成

#### Day 8: 结果收集与上传 ✅
- [x] 实现执行产物收集器
- [x] 实现 MinIO 结果上传
- [x] 更新 FinishTaskRequest DTO
- [x] 端到端测试

#### Day 9: 清理与监控 ✅
- [x] 实现定时清理任务（@Scheduled）
- [x] 添加 Actuator 健康检查
- [x] 添加执行指标埋点
- [x] 文档更新

---

## 四、完整的项目开发路线图

### 4.1 当前推荐开发顺序

```
┌──────────────────────────────────────────────────────────────────┐
│                        第一优先级                                  │
│ ┌─────────────────┐    ┌─────────────────┐                       │
│ │ activation-     │ →  │ JWT 认证授权     │                       │
│ │ runner 产物上传  │    │ (P0 安全基础)   │                       │
│ └─────────────────┘    └─────────────────┘                       │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                        第二优先级                                  │
│ ┌─────────────────┐    ┌─────────────────┐                       │
│ │ 前端工程搭建     │ →  │ 前后端联调        │                       │
│ │ (actrade-web)   │    │ (API 联调)       │                       │
│ └─────────────────┘    └─────────────────┘                       │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                        第三优先级                                  │
│ ┌─────────────────┐    ┌─────────────────┐    ┌───────────────┐ │
│ │ 管理后台接口     │ →  │ 管理前端页面     │ →  │ 测试与部署    │ │
│ └─────────────────┘    └─────────────────┘    └───────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

### 4.2 详细开发任务分解

#### 阶段 1: activation-runner 完善 (Day 1-9) - Day 1-7 已完成 ✅

| 任务 | 依赖 | 工时 | 产出 | 状态 |
|------|------|------|------|------|
| MinIO 客户端集成 | - | 0.5d | MinioConfig.java | ✅ |
| 工具包下载服务 | MinIO 配置 | 1d | PackageDownloadService.java | ✅ |
| SHA-256 校验 | 下载服务 | 0.5d | ChecksumValidator.java | ✅ |
| Zip/Tar 解压 | 下载服务 | 0.5d | ArchiveExtractor.java | ✅ |
| Manifest 解析 | 解压 | 1d | ManifestParser.java | ✅ |
| Docker 命令构建 | - | 1d | DockerCommandBuilder.java | ✅ |
| Docker 执行服务 | 命令构建 | 2d | DockerExecutionService.java | ✅ |
| gVisor 集成 | Docker 服务 | 1d | GVisorProperties.java, DockerConfiguration.java | ✅ |
| 日志采集 | Docker 服务 | 0.5d | LogCollector.java | ✅ |
| 产物上传 | MinIO | 0.5d | ArtifactUploadService.java | 🔴 |
| 集成测试 | 所有组件 | 1d | Integration tests | 🔴 |
| 文档更新 | - | 0.5d | README.md | 🔴 |

#### 阶段 2: JWT 认证授权 (Day 10-14)

| 任务 | 依赖 | 工时 | 产出 |
|------|------|------|------|
| JWT 依赖引入 | - | 0.5d | pom.xml 更新 |
| Token 生成/解析 | - | 1d | JwtService.java |
| 登录/注册接口 | Token 服务 | 1.5d | AuthController.java |
| 密码加密存储 | - | 0.5d | BCrypt 配置 |
| 刷新 Token | Token 服务 | 0.5d | Refresh token 逻辑 |
| Spring Security 配置 | 登录接口 | 1d | SecurityConfig.java |
| @PreAuthorize 注解 | Security 配置 | 1d | 权限注解 |
| Token 黑名单 | Redis | 0.5d | TokenBlacklistService |
| 单元测试 | 所有组件 | 1d | Tests |

#### 阶段 3: 前端工程 (Day 15-28)

| 任务 | 依赖 | 工时 | 产出 |
|------|------|------|------|
| Vue3 项目初始化 | - | 0.5d | actrade-web 目录 |
| Vite + TypeScript | - | 0.5d | 配置文件 |
| Element Plus 引入 | - | 0.5d | UI 组件库 |
| 登录/注册页面 | JWT 完成 | 1d | Auth pages |
| 商品列表页面 | API 文档 | 2d | Product list |
| 商品详情页面 | 列表页 | 1d | Product detail |
| 下单流程 | 详情页 | 2d | Order flow |
| 支付页面 | 支付宝 SDK | 1d | Payment page |
| 订单列表 | - | 1d | Order list |
| 用户中心 | - | 1d | User center |
| API 封装 | 后端 API | 1d | api/ 模块 |
| 状态管理 | - | 1d | Pinia stores |
| 路由守卫 | JWT | 0.5d | Router guard |
| 商家后台 | JWT + 商家权限 | 2d | Merchant pages |

#### 阶段 4: 管理后台 (Day 29-35)

| 任务 | 依赖 | 工时 | 产出 |
|------|------|------|------|
| 商家审核接口 | JWT | 1d | Admin Merchant API |
| 商品审核接口 | JWT | 1d | Admin Product API |
| 用户管理接口 | JWT | 1d | Admin User API |
| 工具审核接口 | JWT | 1d | Admin Tool API |
| 订单管理接口 | JWT | 1d | Admin Order API |
| 统计分析接口 | - | 1d | Admin Stats API |
| 管理后台前端 | 所有接口 | 3d | Admin pages |
| 敏感操作审计 | - | 0.5d | Audit log |
| 操作日志接口 | 审计 | 0.5d | Audit API |

#### 阶段 5: 完善与部署 (Day 36-42)

| 任务 | 依赖 | 工时 | 产出 |
|------|------|------|------|
| Redis 缓存落地 | 业务分析 | 1d | 缓存策略 |
| MinIO 文件上传 | - | 1d | 上传 API |
| 单元测试补充 | 核心代码 | 2d | Tests |
| Docker 镜像构建 | - | 0.5d | Dockerfile |
| Docker Compose 更新 | 镜像 | 0.5d | docker-compose.yml |
| Nginx 配置 | 前端构建 | 0.5d | nginx.conf |
| 部署文档 | 所有配置 | 0.5d | DEPLOY.md |
| 性能优化 | - | 1d | 优化项 |

---

## 五、技术债务与待优化项

### 5.1 已知技术债务

| 债务项 | 严重程度 | 建议处理时机 |
|--------|----------|--------------|
| 无单元测试 | 中 | Phase 2 完成后 |
| 无 API 文档 | 低 | 前后端联调前 |
| activation-runner 无独立数据库 | 低 | 未来扩展时考虑 |
| 无熔断降级 | 中 | 生产部署前 |

### 5.2 安全加固项

| 项 | 优先级 | 说明 |
|----|--------|------|
| activation-runner 网络隔离 | P0 | 不允许访问外网 (已通过 gVisor 配置) |
| 工具包扫描 | P1 | 病毒/恶意代码检测 |
| 执行资源配额 | P1 | 商家级别限制 |
| API 限流 | P2 | 防止滥用 |

---

## 六、环境要求

### 6.1 activation-runner 运行要求

| 组件 | 要求 | 备注 |
|------|------|------|
| Java | 21 | 与 actrade-api 相同 |
| Docker | 20.10+ | Rootless Docker |
| gVisor | 最新稳定版 | runsc runtime |
| MinIO Client | 8.5.17 | 已通过依赖引入 |

### 6.2 Docker 执行权限说明

activation-runner 执行 Docker 需要满足以下任一条件：

1. **开发环境**：当前用户加入 docker 用户组
   ```bash
   sudo usermod -aG docker $USER
   newgrp docker
   ```

2. **容器内运行**：挂载 Docker Socket
   ```yaml
   volumes:
     - /var/run/docker.sock:/var/run/docker.sock
   ```

---

## 七、下一步行动

### 当前建议任务顺序

1. **✅ 已完成**: activation-runner 产物上传功能实现
2. **立即开始**: JWT 认证授权开发
3. **同步进行**: 完善单元测试
4. **Day 9 后**: 前端工程搭建

---

## 八、附录

### A. 相关文档

| 文档 | 位置 |
|------|------|
| activation-runner 详细文档 | `activation-runner/docs/activation-runner文档.md` |
| 项目结构文档 | `documents/项目结构文档.md` |
| 数据库设计 | `actrade-api/src/main/resources/db/migration/` |

### B. API 端点参考

| 端点 | 方法 | 用途 |
|------|------|------|
| `/internal/activation/tasks/claim` | POST | Runner 领取任务 |
| `/internal/activation/tasks/{taskId}/finish` | POST | Runner 提交结果 |
| `/internal/activation/tasks/dispatch` | POST | API 投递任务 (MQ) |

### C. MQ 配置

| 配置项 | 当前值 |
|--------|--------|
| Exchange | activation.task.exchange |
| Queue | activation.task.execute |
| DLX | activation.task.dlx |
| DLQ | activation.task.execute.dlq |

### D. gVisor 集成说明

activation-runner 通过 gVisor runsc 运行时实现容器沙箱隔离：

```java
// Docker 命令中使用 --runtime runsc
docker run --runtime runsc ...

// gVisor 配置通过环境变量传递
RUNSC_FLAG_debug=true
RUNSC_FLAG_trace=true
RUNSC_FLAG_net=sandboxed
```

支持的 gVisor 配置：
- `sandbox-type`: application/hostinet
- `file-system-type`: overlay/passthrough
- `network-mode`: sandboxed/hostinet/none
- `trace-syscall`: 系统调用跟踪
- `seccomp-enabled`: seccomp 过滤

---

## 九、JWT 认证授权（Phase 2.5）✅ 已完成

### 9.1 已实现组件

| 组件 | 文件路径 | 说明 |
|------|---------|------|
| JWT 配置属性 | `config/JwtProperties.java` | Token 过期时间、Secret 等配置 |
| Token 提供者 | `security/JwtTokenProvider.java` | Token 生成/解析/验证 |
| 用户详情服务 | `security/CustomUserDetailsService.java` | Spring Security 用户加载 |
| JWT 认证过滤器 | `security/JwtAuthenticationFilter.java` | 请求拦截 Token 鉴权 |
| 内部 Token 过滤器 | `security/InternalTokenFilter.java` | /internal/** 接口认证 |
| 认证服务 | `security/AuthService.java` | 登录/注册/刷新/登出逻辑 |
| 认证控制器 | `security/controller/AuthController.java` | REST API 端点 |
| Token 黑名单 | `security/TokenBlacklistService.java` | Redis 实现登出黑名单 |
| DTO 类 | `security/dto/*.java` | 请求/响应对象 |

### 9.2 API 端点

| 端点 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/api/v1/auth/login` | POST | 用户登录 | 无 |
| `/api/v1/auth/register` | POST | 用户注册 | 无 |
| `/api/v1/auth/refresh` | POST | 刷新 Token | 无 |
| `/api/v1/auth/me` | GET | 获取当前用户 | Bearer Token |
| `/api/v1/auth/logout` | POST | 用户登出 | Bearer Token |

### 9.3 请求/响应示例

#### 登录
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

#### 响应
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "testuser",
  "role": "ROLE_USER"
}
```

### 9.4 内部接口认证

activation-runner 调用 actrade-api 使用 `X-Internal-Token` 头：

```bash
curl -X POST http://localhost:8080/internal/activation/tasks/claim \
  -H "X-Internal-Token: actrade-internal-token-change-this-in-production-environment"
```

### 9.5 角色权限体系

| 角色 | 说明 | 可访问接口 |
|------|------|-----------|
| ROLE_USER | 普通用户 | 个人订单、激活任务 |
| ROLE_MERCHANT | 商户 | 商品管理、激活任务 |
| ROLE_ADMIN | 管理员 | 所有接口 |
| ROLE_INTERNAL | 内部服务 | /internal/** 路径 |

### 9.6 环境配置

```bash
# JWT 配置
JWT_SECRET=actrade-jwt-secret-key-must-be-at-least-32-characters-long-for-hs256-algorithm
JWT_ACCESS_EXPIRATION=1800000    # 30 分钟
JWT_REFRESH_EXPIRATION=604800000 # 7 天

# 内部服务 Token
INTERNAL_SERVICE_TOKEN=actrade-internal-token-change-this-in-production-environment
```

### 9.7 单元测试

JWT Token Provider 测试：`src/test/java/com/masterhesse/security/JwtTokenProviderTest.java`

**测试结果：✅ 全部通过 (17 tests)**

| 测试类 | 测试用例数 | 结果 |
|--------|-----------|------|
| Token 生成测试 | 4 | ✅ 全部通过 |
| Token 解析测试 | 4 | ✅ 全部通过 |
| Token 验证测试 | 5 | ✅ 全部通过 |
| Token 过期测试 | 2 | ✅ 全部通过 |
| Token 类型检查测试 | 2 | ✅ 全部通过 |

测试覆盖：
- ✅ Token 生成测试 (Access Token、Refresh Token、用户名生成)
- ✅ Token 解析测试 (用户名提取、角色提取、类型获取、Claims 解析)
- ✅ Token 验证测试 (有效/无效/伪造 Token)
- ✅ Token 过期测试 (新 Token 不过期、空 Token 拒绝)
- ✅ Token 类型检查 (Access/Refresh 区分)

### 9.8 编译环境修复

**问题**：Maven 本地仓库路径配置错误导致依赖无法正确解析

**解决方案**：创建 `/home/hesse/.m2/settings.xml` 配置正确的本地仓库路径

```xml
<localRepository>/home/hesse/.m2/repository</localRepository>
```

### 9.9 下一步

- [ ] 启动 actrade-api 服务进行集成测试
- [ ] 测试登录/注册 API
- [ ] 测试 Runner 与 API 的内部接口通信

---

## 十、M2 Runner健康检查与可观测性 ✅ 已完成

### 10.1 已实现组件

| 组件 | 文件路径 | 说明 |
|------|---------|------|
| Prometheus 依赖 | `pom.xml` | micrometer-registry-prometheus |
| Runner 指标收集器 | `metrics/RunnerMetrics.java` | Micrometer Counter/Gauge/Timer |
| Docker 健康检查 | `health/DockerHealthIndicator.java` | Docker 连接与镜像状态检查 |
| MinIO 健康检查 | `health/MinioHealthIndicator.java` | MinIO 连接与 Bucket 状态检查 |
| RabbitMQ 健康检查 | `health/RabbitMQHealthIndicator.java` | RabbitMQ 连接与队列状态检查 |
| Actuator 配置 | `application.yaml` | Prometheus 端点、健康检查分组 |
| 消息监听指标 | `mq/ActivationTaskMessageListener.java` | 集成 Micrometer 埋点 |

### 10.2 健康检查端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/actuator/health` | GET | 总体健康检查 |
| `/actuator/health/docker` | GET | Docker 服务健康检查 |
| `/actuator/health/minio` | GET | MinIO 服务健康检查 |
| `/actuator/health/rabbit` | GET | RabbitMQ 服务健康检查 |
| `/actuator/prometheus` | GET | Prometheus 指标导出 |

### 10.3 自定义健康检查指示器

#### DockerHealthIndicator
```java
// 检查项
- Docker daemon 连接状态
- Docker API 版本兼容性
- 容器运行时可用性
- runsc (gVisor) 运行时检测（可选）

// 状态返回
- Health.up(): Docker 完全可用
- Health.unknown(): Docker 可连接但运行时检测失败
- Health.down(): Docker 连接失败
```

#### MinioHealthIndicator
```java
// 检查项
- MinIO 服务可达性
- artifacts-bucket 存在状态
- results-bucket 存在状态

// 状态返回
- Health.up(): 服务可达且所有 Bucket 存在
- Health.unknown(): 服务可达但 Bucket 缺失（自动创建可能已启用）
- Health.down(): 服务不可达或连接失败
```

#### RabbitMQHealthIndicator
```java
// 检查项
- RabbitMQ 服务器连接
- 虚拟主机存在性
- 任务队列 (activation.task.execute) 状态
- 死信队列 (activation.task.execute.dlq) 状态

// 状态返回
- Health.up(): RabbitMQ 完全可用
- Health.unknown(): 部分队列状态异常
- Health.down(): RabbitMQ 连接失败
```

### 10.4 Prometheus 指标

#### RunnerMetrics 定义的指标

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `runner_tasks_received_total` | Counter | 接收到的任务总数 |
| `runner_tasks_started_total` | Counter | 开始执行的任务数 |
| `runner_tasks_completed_total` | Counter | 成功完成的任务数 |
| `runner_tasks_failed_total` | Counter | 执行失败的任务数 |
| `runner_tasks_retried_total` | Counter | 重试的任务数 |
| `runner_task_execution_seconds` | Timer | 任务执行耗时分布 |
| `runner_workspace_active_count` | Gauge | 当前活跃工作空间数量 |

#### MessageListener 埋点

| 事件 | 指标 | 说明 |
|------|------|------|
| 任务接收 | `runner_tasks_received_total` | 每接收一条消息 +1 |
| 任务开始 | `runner_tasks_started_total` | 任务开始执行时 +1 |
| 任务完成 | `runner_task_execution_seconds` | 记录执行耗时 |
| 任务失败 | `runner_tasks_failed_total` | 执行异常时 +1 |
| 任务重试 | `runner_tasks_retried_total` | 消息重试时 +1 |

### 10.5 Actuator 配置

```yaml
# activation-runner/application.yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState,minio,rabbit,docker
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.90, 0.95, 0.99
```

### 10.6 健康检查语义规范

| 返回状态 | 含义 | 使用场景 |
|---------|------|---------|
| `Health.up()` | 完全正常 | 服务和所有依赖项均可用 |
| `Health.unknown()` | 状态不确定 | 服务可达但有配置问题（如 Bucket 缺失） |
| `Health.down()` | 完全不可用 | 关键依赖不可用 |

---

## 十一、M3 Vue3前端工程 ✅ 已完成

### 11.1 项目结构

```
actrade-web/
├── package.json          # 依赖配置 (Vue 3.5, Vite 6, Element Plus等)
├── vite.config.ts        # Vite构建配置，含API代理
├── tsconfig.json         # TypeScript配置
├── index.html            # 入口HTML
├── .env.development      # 开发环境变量
├── .env.production       # 生产环境变量
├── .gitignore
└── src/
    ├── main.ts           # 应用入口
    ├── App.vue            # 根组件
    ├── env.d.ts          # 环境变量类型声明
    ├── router/
    │   └── index.ts      # Vue Router配置
    ├── stores/
    │   └── auth.ts       # Pinia认证状态管理
    ├── api/
    │   ├── axios.ts      # Axios实例配置
    │   ├── auth.ts       # 认证API
    │   └── product.ts    # 商品/订单API
    ├── layouts/
    │   └── MainLayout.vue # 主布局组件
    └── views/
        ├── Login.vue         # 登录页
        ├── Register.vue      # 注册页
        ├── Products.vue       # 商品列表页
        ├── ProductDetail.vue  # 商品详情页
        ├── Orders.vue         # 订单页
        ├── UserCenter.vue     # 用户中心
        └── NotFound.vue       # 404页面
```

### 11.2 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5+ | 渐进式前端框架 |
| Vite | 6+ | 下一代前端构建工具 |
| TypeScript | 5+ | JavaScript 超集 |
| Vue Router | 4+ | 路由管理 |
| Pinia | 3+ | 状态管理 |
| Element Plus | 2+ | UI 组件库 |
| Axios | 1+ | HTTP 客户端 |

### 11.3 已实现页面

| 页面 | 路径 | 功能 |
|------|------|------|
| 登录页 | `/login` | 用户登录 |
| 注册页 | `/register` | 用户注册 |
| 商品列表 | `/products` | 商品浏览与搜索 |
| 商品详情 | `/products/:id` | 商品详情与购买 |
| 订单列表 | `/orders` | 订单管理 |
| 用户中心 | `/user` | 个人信息与设置 |
| 404页面 | `/*` | 页面未找到 |

### 11.4 API 封装

#### Axios 实例配置
```typescript
// src/api/axios.ts
- 基础URL配置: `/api/v1`
- 请求拦截器: 自动添加 JWT Token
- 响应拦截器: 统一错误处理
- 超时配置: 30秒
- 重试机制: 3次
```

#### 认证 API
```typescript
// src/api/auth.ts
- login(data) → POST /auth/login
- register(data) → POST /auth/register
- refresh(data) → POST /auth/refresh
- logout() → POST /auth/logout
- getCurrentUser() → GET /auth/me
```

#### 商品/订单 API
```typescript
// src/api/product.ts
- getProducts(params?) → GET /products
- getProduct(id) → GET /products/{id}
- createOrder(data) → POST /orders
- getOrders() → GET /orders
- getOrder(id) → GET /orders/{id}
```

### 11.5 状态管理

#### Auth Store (Pinia)
```typescript
// src/stores/auth.ts
- state: user, token, isAuthenticated
- getters: isLoggedIn, userRole
- actions: login(), register(), logout(), fetchUser()
```

### 11.6 路由配置

```typescript
// src/router/index.ts
- 路由守卫: 检查登录状态和 Token 有效期
- 登录页/注册页: 公开访问
- 其他页面: 需要登录
- 404: 捕获所有未匹配路由
```

### 11.7 环境变量

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_APP_TITLE=ACTrade 开发环境

# .env.production
VITE_API_BASE_URL=/api/v1
VITE_APP_TITLE=ACTrade
```

### 11.8 启动前端项目

```bash
# 安装依赖
cd actrade-web
npm install

# 开发模式
npm run dev

# 构建生产版本
npm run build

# 预览生产版本
npm run preview
```

---

## 十二、项目整体进度总结

### 已完成阶段

| 阶段 | 状态 | 完成日期 |
|------|------|---------|
| M1 核心业务与基础设施 | ✅ 完成 | - |
| M2 Runner健康检查与可观测性 | ✅ 完成 | 2026-04-07 |
| M3 Vue3前端工程 | ✅ 完成 | 2026-04-07 |
| JWT 认证授权 | ✅ 完成 | 2026-04-07 |

### 下一步计划

| 优先级 | 任务 | 说明 |
|--------|------|------|
| P1 | **自动发货功能修复** | 支付成功后激活码自动发放逻辑 |
| P1 | 前后端联调 | API 联调测试 |
| P2 | 管理后台接口 | 后台管理功能开发 |
| P2 | 单元测试补充 | 核心业务测试覆盖 |
| P2 | 端到端测试 | 完整业务流程测试 |
| P3 | Docker 镜像构建 | 生产部署准备 |
| P3 | 部署文档 | DEPLOY.md |

---

## 十三、M4 支付流程修复 ✅ 已完成 (2026-04-09)

### 13.1 问题背景

用户在完成支付宝支付后，系统前端仍显示"未支付"状态。后端 ngrok 日志显示支付回调接口 `/api/payments/alipay/return` 返回 403 错误。

### 13.2 问题分析与解决方案

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 403 错误 | `SecurityConfig` 未配置 `/api/payments/alipay/**` 为公开访问 | 添加 `.requestMatchers("/api/payments/alipay/**").permitAll()` |
| 支付状态不更新 | 同步回调（return）只返回 HTML 页面，未调用支付状态更新逻辑 | 在 `return` 接口中调用 `handleAlipayNotify` 更新状态 |
| 登录状态丢失 | 在当前标签页跳转支付页面，刷新后丢失会话 | 改用 `window.open` 在新标签页打开 |
| formHtml 传递失败 | URL query 参数传递 HTML 被截断或编码破坏 | 改用 `sessionStorage` 存储支付数据 |

### 13.3 修改文件汇总

| 文件 | 修改内容 |
|------|---------|
| `SecurityConfig.java` | 添加支付回调路径公开访问配置 |
| `AlipayCallbackController.java` | 1. `return` 接口添加支付状态处理逻辑<br>2. 支付成功时显示绿色标题<br>3. 添加 3 秒倒计时自动跳转<br>4. 支持 opener 窗口刷新关闭 |
| `router/index.ts` | 将 `/payment-gateway` 路由移到 `MainLayout` 外部作为独立路由 |
| `OrderDetail.vue` | 1. `handlePay` 改用 `sessionStorage` 存储 formHtml<br>2. 改用 `window.open` 在新标签页打开支付网关<br>3. 添加"支付中"状态面板（`paymentStatus === 'PAYING'`） |
| `PaymentGateway.vue` | 从 `sessionStorage` 读取 `formHtml`，移除 URL query 依赖 |

### 13.4 支付流程

```
1. 用户在订单详情页点击"确认支付" → 选择支付宝
2. 前端调用 initiatePayment 接口
3. 后端返回包含 formHtml 的响应
4. formHtml 存入 sessionStorage → 在新标签页打开 /payment-gateway
5. 支付网关页面从 sessionStorage 读取 formHtml → 自动提交表单 → 支付宝沙箱
6. 用户完成支付后：
   - 支付宝异步通知 notify 接口 → 更新订单状态
   - 支付宝同步跳转 return 接口 → 也尝试更新状态 → 显示结果页面
7. 3 秒后自动跳转回订单页面（有 opener 则刷新 opener 后关闭）
8. 用户切换回原标签页刷新订单状态
```

### 13.5 编译验证

- ✅ 后端编译：`BUILD SUCCESS`
- ✅ 前端编译：`✓ built in 5.18s`

---

## 十四、M5 自动发货功能 🔄 进行中

### 14.1 功能概述

自动发货是指用户支付成功后，系统自动完成以下操作：
1. 更新订单状态为"已支付"
2. 根据商品的发货方式（自动/手动），自动分配激活码或触发激活任务

### 14.2 当前状态

| 组件 | 状态 | 说明 |
|------|------|------|
| 支付状态更新 | ✅ 已完成 | 支付宝回调后订单状态正确更新为 PAID |
| 激活码分配 | 🔄 待修复 | 支付成功后激活码自动发放逻辑 |
| 激活任务触发 | 🔄 待修复 | MQ 消息投递与 Runner 执行 |
| 前端状态展示 | ✅ 已完成 | 订单详情页显示支付状态和发货状态 |

### 14.3 待排查问题

1. **支付成功事件未触发发货逻辑**：需要检查 `OrderPaidEvent` 事件监听器是否正确处理
2. **激活码库存不足**：需要检查激活码生成和分配逻辑
3. **MQ 消息投递失败**：需要检查 RabbitMQ 配置和消息监听器

### 14.4 排查建议

```bash
# 1. 检查后端日志
tail -f activation-runner.log | grep -i "paid\|event\|fulfill"

# 2. 检查 MQ 消息
curl http://localhost:15672/api/queues (需要 RabbitMQ Management Plugin)

# 3. 检查数据库订单状态
psql -h localhost -U actrade -d actrade -c "SELECT order_id, order_status, payment_status FROM orders LIMIT 10;"

# 4. 检查激活码库存
psql -h localhost -U actrade -d actrade -c "SELECT product_id, COUNT(*) FROM activation_codes GROUP BY product_id;"
```

### 14.5 下一步修复计划

| 优先级 | 任务 | 说明 |
|--------|------|------|
| P1 | 检查 OrderPaidEvent 事件监听器 | 确认支付成功事件正确触发 |
| P1 | 验证激活码分配逻辑 | 检查库存扣减和激活码生成 |
| P1 | 检查 MQ 消息投递 | 确认激活任务消息正确发送 |
| P2 | 前端发货状态展示 | 优化用户查看发货进度体验 |
