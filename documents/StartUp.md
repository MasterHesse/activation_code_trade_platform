# ACTrade 系统部署与运行指南

> 文档版本：1.0
> 更新日期：2026-04-07
> 适用版本：✅ M3 阶段已完成
> - M1 核心业务与基础设施：✅ 已完成
> - M2 Runner健康检查与可观测性：✅ 已完成
> - M3 Vue3前端工程：✅ 已完成

---

## 一、环境依赖配置

### 1.1 基础环境要求

| 组件 | 最低版本 | 推荐版本 | 说明 |
|------|----------|----------|------|
| JDK | 21 | 21 LTS | Java 运行环境 |
| Maven | 3.9+ | 3.9.6 | 项目构建工具 |
| Docker | 20.10+ | 24.0+ | 容器运行时 |
| Docker Compose | 2.0+ | 2.20+ | 容器编排工具 |
| Git | 2.30+ | 最新 | 版本控制 |

### 1.2 必需服务

| 服务 | 版本 | 用途 | 端口 |
|------|------|------|------|
| PostgreSQL | 15+ | 主数据库 | 5432 |
| Redis | 7+ | 缓存/消息队列 | 6379 |
| RabbitMQ | 3.12+ | 消息队列 | 5672/15672 |
| MinIO | RELEASE.2023+ | 对象存储 | 9000/9001 |

### 1.3 可选服务（gVisor 沙箱）

| 组件 | 版本 | 说明 |
|------|------|------|
| gVisor (runsc) | 最新稳定版 | 容器沙箱运行时，提供更强的隔离 |
| runsc-init | - | runsc 初始化工具 |

---

## 二、环境安装步骤

### 2.1 安装 Java 21

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk

# 验证安装
java -version
# openjdk version "21.x.x" ...

# 设置 JAVA_HOME
echo "export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64" >> ~/.bashrc
source ~/.bashrc
```

### 2.2 安装 Docker

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh

# 当前用户加入 docker 组
sudo usermod -aG docker $USER
newgrp docker

# 验证安装
docker --version
docker compose version
```

### 2.3 安装 Docker Compose

```bash
# 下载最新版本
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 添加执行权限
sudo chmod +x /usr/local/bin/docker-compose

# 创建符号链接
sudo ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# 验证安装
docker-compose --version
```

### 2.4 安装 gVisor (可选但推荐)

```bash
# 下载 gVisor
curl -fsSL https://gvisor.dev/archive.key | sudo apt-key add -
echo "deb https://storage.googleapis.com/docker-toolbox-linux/apt amd64 main" | sudo tee /etc/apt/sources.list.d/docker-toolbox.list
sudo apt update
sudo apt install runsc

# 验证安装
runsc --version

# 配置 Docker 使用 runsc
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<EOF
{
  "runtimes": {
    "runsc": {
      "path": "/usr/bin/runsc",
      "runtimeArgs": ["--platform=ptrace"]
    }
  }
}
EOF

# 重启 Docker
sudo systemctl restart docker

# 验证 runsc 可用
docker info | grep -A 10 Runtimes
```

### 2.5 启动基础设施服务

```bash
# 进入项目目录
cd /home/hesse/dev/activation_code_trade_platform

# 启动基础服务（PostgreSQL, Redis, RabbitMQ, MinIO）
docker-compose -f docker-compose.yml up -d

# 查看服务状态
docker-compose -f docker-compose.yml ps

# 查看日志
docker-compose -f docker-compose.yml logs -f
```

---

## 三、项目配置

### 3.1 数据库初始化

```bash
# 创建数据库
docker exec -it actrade-postgres psql -U postgres -c "CREATE DATABASE actrade;"

# Flyway 迁移会自动执行（首次启动时）
```

### 3.2 配置 application.yml

#### actrade-api 配置

```yaml
# actrade-api/src/main/resources/application.yml
spring:
  application:
    name: actrade-api

  datasource:
    url: jdbc:postgresql://localhost:5432/actrade
    username: postgres
    password: your_password_here

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

  data:
    redis:
      host: localhost
      port: 6379

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: actrade
```

#### activation-runner 配置

```yaml
# activation-runner/src/main/resources/application.yml
spring:
  application:
    name: activation-runner

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

  data:
    redis:
      host: localhost
      port: 6379

actrade:
  api:
    base-url: http://localhost:8080
    internal-api-key: your_api_key_here

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: actrade

docker:
  binary: docker
  runtime: runsc  # 或 docker 默认运行时
  image:
    shell: ubuntu:22.04
  default-memory-mb: 512
  default-cpu-limit: 1.0
  pids-limit: 128
  default-timeout-seconds: 300
  network-disabled: true

docker.gvisor:
  enabled: true
  runsc-path: /usr/bin/runsc
  runtime-name: runsc
  sandbox-type: application
  file-system-type: overlay
  network-mode: sandboxed
  network-disabled: false
  trace-syscall: false
  debug: false
  log-format: text
  debug-log-path: /var/log/runsc
  heartbeat-interval-ms: 30000
  test-mode-enabled: false
  cgo-tracing-enabled: false

runner:
  workspace:
    base-dir: /tmp/actrade-runner
    max-age-hours: 24
  cleanup:
    enabled: true
    cron: "0 0 2 * * ?"

# M2 可观测性配置
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
```

### 3.3 配置环境变量

```bash
# 创建环境变量文件
cat > .env <<EOF
# 数据库
POSTGRES_PASSWORD=your_secure_password

# Redis
REDIS_PASSWORD=your_redis_password

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin

# API 密钥
INTERNAL_API_KEY=your_internal_api_key
EOF
```

---

## 四、服务启动步骤

### 4.1 启动 actrade-api

```bash
# 进入 actrade-api 目录
cd actrade-api

# 编译项目
./mvnw clean package -DskipTests

# 运行应用
./mvnw spring-boot:run

# 或者使用 JAR 文件运行
java -jar target/actrade-api-1.0.0.jar
```

**验证 actrade-api 启动成功：**
```bash
curl http://localhost:8080/actuator/health
# 期望返回：{"status":"UP"}
```

### 4.2 启动 activation-runner

```bash
# 新开终端，进入 activation-runner 目录
cd activation-runner

# 编译项目
./mvnw clean package -DskipTests

# 运行应用
./mvnw spring-boot:run

# 或者使用 JAR 文件运行
java -jar target/activation-runner-1.0.0.jar
```

**验证 activation-runner 启动成功：**
```bash
curl http://localhost:8081/actuator/health
# 期望返回：{"status":"UP"}
```

### 4.3 使用 Docker Compose 启动所有服务

```bash
# 在项目根目录执行
docker-compose up -d

# 查看所有服务状态
docker-compose ps

# 查看日志
docker-compose logs -f actrade-api
docker-compose logs -f activation-runner
```

---

## 五、必要验证命令

### 5.1 Docker 验证

```bash
# 验证 Docker 可用
docker ps

# 验证 runsc 可用（如果已安装 gVisor）
docker info | grep runsc

# 测试 Docker 拉取镜像
docker pull ubuntu:22.04

# 测试容器运行
docker run --rm hello-world
```

### 5.2 gVisor 验证

```bash
# 验证 runsc 可执行
runsc --version

# 测试 gVisor 容器
docker run --rm --runtime runsc ubuntu:22.04 echo "Hello from gVisor"
```

### 5.3 服务连接验证

```bash
# 验证 PostgreSQL 连接
docker exec -it actrade-postgres psql -U postgres -c "SELECT version();"

# 验证 Redis 连接
docker exec -it actrade-redis redis-cli ping
# 期望返回：PONG

# 验证 RabbitMQ 连接
docker exec -it actrade-rabbitmq rabbitmq-diagnostics ping

# 验证 MinIO 连接
mc alias set local http://localhost:9000 minioadmin minioadmin
mc ls local/
```

### 5.4 应用健康检查

```bash
# actrade-api 健康检查
curl -s http://localhost:8080/actuator/health | jq

# activation-runner 健康检查
curl -s http://localhost:8081/actuator/health | jq

# activation-runner Docker 可用性检查
curl http://localhost:8081/actuator/health/docker | jq

# activation-runner MinIO 可用性检查 (M2 新增)
curl http://localhost:8081/actuator/health/minio | jq

# activation-runner RabbitMQ 可用性检查 (M2 新增)
curl http://localhost:8081/actuator/health/rabbit | jq

# activation-runner Prometheus 指标端点 (M2 新增)
curl http://localhost:8081/actuator/prometheus | jq

# activation-runner 完整健康检查详情
curl http://localhost:8081/actuator/health?showDetails=always | jq
```

### 5.5 端到端测试

```bash
# 创建测试任务（通过 API）
curl -X POST http://localhost:8080/internal/activation/tasks/dispatch \
  -H "Content-Type: application/json" \
  -H "X-Internal-Api-Key: your_internal_api_key" \
  -d '{
    "toolId": "test-tool-001",
    "payload": {"input": "test data"}
  }'

# 检查 RabbitMQ 队列
docker exec -it actrade-rabbitmq rabbitmqctl list_queues
```

---

## 六、日志查看

### 6.1 Docker Compose 日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f actrade-api
docker-compose logs -f activation-runner

# 查看最近 100 行日志
docker-compose logs --tail=100
```

### 6.2 应用日志

```bash
# actrade-api 日志位置
tail -f actrade-api/logs/actrade-api.log

# activation-runner 日志位置
tail -f activation-runner/logs/activation-runner.log

# 查看 gVisor 日志（如果启用）
tail -f /var/log/runsc/runsc.log
```

### 6.3 容器执行日志

```bash
# 查看容器执行输出
cat /tmp/actrade-runner/*/logs/stdout.log
cat /tmp/actrade-runner/*/logs/stderr.log
```

---

## 七、常见问题排查

### 7.1 Docker 权限问题

**症状：** `permission denied while trying to connect to the Docker daemon`

**解决方案：**
```bash
# 将当前用户加入 docker 组
sudo usermod -aG docker $USER
# 重新登录或执行
newgrp docker
```

### 7.2 gVisor 未找到

**症状：** `Error response from daemon: Unknown runtime specified: runsc`

**解决方案：**
```bash
# 确认 runsc 已安装
which runsc

# 重新配置 Docker
sudo systemctl restart docker

# 或者手动添加运行时
dockerd --add-runtime runsc=/usr/bin/runsc &
```

### 7.3 端口冲突

**症状：** `Bind for 0.0.0.0:8080 failed: port is already allocated`

**解决方案：**
```bash
# 查找占用端口的进程
sudo lsof -i :8080

# 停止冲突进程或修改配置使用其他端口
```

### 7.4 数据库连接失败

**症状：** `Connection refused. Check that the hostname and port are correct`

**解决方案：**
```bash
# 确认 PostgreSQL 正在运行
docker-compose ps postgres

# 检查网络连接
docker exec -it actrade-postgres pg_isready

# 重启 PostgreSQL
docker-compose restart postgres
```

### 7.5 MinIO 连接失败

**症状：** `Unable to execute HTTP request: Connection refused`

**解决方案：**
```bash
# 确认 MinIO 正在运行
docker-compose ps minio

# 检查 MinIO 健康状态
curl http://localhost:9000/minio/health/live
```

---

## 八、快速启动脚本

```bash
#!/bin/bash
# quick-start.sh - 快速启动脚本

set -e

echo "=== ACTrade 系统快速启动 ==="

# 1. 启动基础设施
echo "[1/5] 启动基础设施服务..."
docker-compose up -d postgres redis rabbitmq minio

# 等待服务就绪
echo "等待服务启动..."
sleep 10

# 2. 验证服务
echo "[2/5] 验证服务状态..."
docker-compose ps

# 3. 启动 actrade-api
echo "[3/5] 启动 actrade-api..."
cd actrade-api
mvn spring-boot:run &
cd ..

# 等待 actrade-api 启动
sleep 15

# 4. 启动 activation-runner
echo "[4/5] 启动 activation-runner..."
cd activation-runner
mvn spring-boot:run &
cd ..

# 5. 启动前端开发服务器 (可选)
echo "[5/5] 启动前端开发服务器..."
cd actrade-web
npm run dev &
cd ..

echo "=== 启动完成 ==="
echo "actrade-api: http://localhost:8080"
echo "activation-runner: http://localhost:8081"
echo "前端开发服务器: http://localhost:5173"
echo "MinIO Console: http://localhost:9001"
echo "RabbitMQ: http://localhost:15672"
```

---

## 九、联系方式与支持

- 项目文档：`/documents/ProjectFile20260407.md`
- activation-runner 详细文档：`activation-runner/docs/activation-runner文档.md`
- 项目结构文档：`documents/项目结构文档.md`

---

## 十、M2 可观测性配置

### 10.1 Prometheus 指标端点

```bash
# 获取 Prometheus 格式的指标
curl http://localhost:8081/actuator/prometheus

# 常用指标查询
curl "http://localhost:8081/actuator/metrics/runner_tasks_received_total"
curl "http://localhost:8081/actuator/metrics/runner_tasks_completed_total"
curl "http://localhost:8081/actuator/metrics/runner_task_execution_seconds"
```

### 10.2 可用健康检查组件

| 检查项 | 端点 | 说明 |
|--------|------|------|
| Docker | `/actuator/health/docker` | Docker daemon 连接状态 |
| MinIO | `/actuator/health/minio` | MinIO 服务及 Bucket 状态 |
| RabbitMQ | `/actuator/health/rabbit` | 消息队列连接及队列状态 |
| Liveness | `/actuator/health/liveness` | 应用存活探针 |
| Readiness | `/actuator/health/readiness` | 应用就绪探针 |

### 10.3 Grafana 集成建议

```yaml
# prometheus.yml 配置示例
scrape_configs:
  - job_name: 'actrade-runner'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']
    scrape_interval: 15s
```

---

## 十一、M3 Vue3前端工程启动

### 11.1 前端环境要求

| 组件 | 最低版本 | 推荐版本 |
|------|----------|----------|
| Node.js | 18+ | 20 LTS |
| npm | 9+ | 10+ |

### 11.2 启动前端开发服务器

```bash
# 进入前端项目目录
cd /home/hesse/dev/activation_code_trade_platform/actrade-web

# 安装依赖（如尚未安装）
npm install

# 启动开发服务器
npm run dev

# 访问地址: http://localhost:5173
```

### 11.3 前端构建命令

```bash
# 开发环境构建
npm run build

# 生产环境构建
npm run build

# 预览生产构建
npm run preview

# 类型检查
npm run type-check

# 代码检查
npm run lint
```

### 11.4 前端环境变量

```bash
# 开发环境 (.env.development)
VITE_API_BASE_URL=http://localhost:8080/api/v1

# 生产环境 (.env.production)
VITE_API_BASE_URL=/api/v1
```

### 11.5 API 代理配置

Vite 开发服务器配置了 `/api` 代理到后端服务：

```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 11.6 前端页面访问

| 页面 | URL | 说明 |
|------|-----|------|
| 首页/商品列表 | `/` | 商品浏览 |
| 登录 | `/login` | 用户登录 |
| 注册 | `/register` | 用户注册 |
| 商品详情 | `/products/:id` | 查看商品详情 |
| 我的订单 | `/orders` | 订单管理 |
| 用户中心 | `/user` | 个人设置 |

---

## 十二、快速启动完整流程

```bash
#!/bin/bash
# complete-startup.sh - 完整启动脚本

set -e

echo "=== ACTrade 系统完整启动 ==="

# 1. 启动基础设施
echo "[1/5] 启动基础设施服务..."
docker-compose up -d postgres redis rabbitmq minio
sleep 10

# 2. 启动 actrade-api
echo "[2/5] 启动 actrade-api..."
cd actrade-api
mvn spring-boot:run &
cd ..

# 3. 启动 activation-runner
echo "[3/5] 启动 activation-runner..."
cd activation-runner
mvn spring-boot:run &
cd ..

# 4. 启动前端开发服务器
echo "[4/5] 启动前端开发服务器..."
cd actrade-web
npm run dev &
cd ..

# 5. 验证服务
echo "[5/5] 验证服务状态..."
sleep 15
echo "--- actrade-api ---"
curl -s http://localhost:8080/actuator/health | jq
echo "--- activation-runner ---"
curl -s http://localhost:8081/actuator/health | jq
echo "--- 前端 ---"
echo "访问 http://localhost:5173"

echo "=== 启动完成 ==="
```
