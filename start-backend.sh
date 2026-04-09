#!/bin/bash
# start-backend.sh - 启动后端服务

set -e

PROJECT_ROOT="/home/hesse/dev/activation_code_trade_platform"
INFRA_DIR="$PROJECT_ROOT/infra"
API_DIR="$PROJECT_ROOT/actrade-api"
RUNNER_DIR="$PROJECT_ROOT/activation-runner"

echo "=== 加载环境变量 ==="
set -a
source "$INFRA_DIR/api.env"
set +a

echo "--- actrade-api 启动中 (端口 8080) ---"
cd "$API_DIR"
java -jar target/demo-0.0.1-SNAPSHOT.jar &
API_PID=$!

echo "等待 actrade-api 启动..."
sleep 20

if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ actrade-api 已就绪"
else
    echo "❌ actrade-api 启动失败，请检查日志"
fi

echo ""
echo "--- activation-runner 启动中 (端口 8081) ---"
cd "$RUNNER_DIR"
# 创建工作目录并设置权限
mkdir -p /home/hesse/dev/activation-runner-workspace
# 启动 runner，使用本地工作目录
java -jar target/activation-runner-0.0.1-SNAPSHOT.jar \
  --server.port=8081 \
  -Drunner.workspace.base-dir=/home/hesse/dev/activation-runner-workspace &
RUNNER_PID=$!

echo "等待 activation-runner 启动..."
sleep 15

if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "✅ activation-runner 已就绪"
else
    echo "❌ activation-runner 启动失败，请检查日志"
fi

echo ""
echo "=== 后端服务启动完成 ==="
echo "actrade-api: http://localhost:8080"
echo "activation-runner: http://localhost:8081"
echo "API PID: $API_PID"
echo "Runner PID: $RUNNER_PID"
