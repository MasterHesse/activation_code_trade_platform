#!/bin/bash
# start-services.sh - 启动所有服务（持续运行）

PROJECT_ROOT="/home/hesse/dev/activation_code_trade_platform"
INFRA_DIR="$PROJECT_ROOT/infra"
API_DIR="$PROJECT_ROOT/actrade-api"
RUNNER_DIR="$PROJECT_ROOT/activation-runner"

echo "=== 启动 actrade-api ==="
cd "$API_DIR"
source "$INFRA_DIR/api.env"
nohup java -jar target/demo-0.0.1-SNAPSHOT.jar > /tmp/actrade-api.log 2>&1 &
API_PID=$!
echo "actrade-api PID: $API_PID"

echo "等待 actrade-api 启动..."
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ actrade-api 已就绪 (PID: $API_PID)"
        break
    fi
    sleep 1
done

echo ""
echo "=== 启动 activation-runner ==="
cd "$RUNNER_DIR"
source "$INFRA_DIR/runner.env"
nohup java -jar target/activation-runner-0.0.1-SNAPSHOT.jar --server.port=8081 > /tmp/activation-runner.log 2>&1 &
RUNNER_PID=$!
echo "activation-runner PID: $RUNNER_PID"

echo "等待 activation-runner 启动..."
for i in {1..30}; do
    if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
        echo "✅ activation-runner 已就绪 (PID: $RUNNER_PID)"
        break
    fi
    sleep 1
done

echo ""
echo "=== 服务状态 ==="
echo "actrade-api:      http://localhost:8080"
echo "actrade-api log:  /tmp/actrade-api.log"
echo ""
echo "activation-runner: http://localhost:8081"
echo "activation-runner log: /tmp/activation-runner.log"
echo ""
echo "=== 停止服务 ==="
echo "pkill -f 'demo-0.0.1-SNAPSHOT' && pkill -f 'activation-runner-0.0.1-SNAPSHOT'"
