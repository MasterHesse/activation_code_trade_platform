#!/bin/bash
# start-frontend.sh - 启动前端开发服务器

set -e

FRONTEND_DIR="/home/hesse/dev/activation_code_trade_platform/actrade-web"

echo "=== 检查 Node.js ==="
if ! command -v node &> /dev/null; then
    echo "❌ Node.js 未安装"
    exit 1
fi

echo "Node.js 版本: $(node -v)"
echo "npm 版本: $(npm -v)"

echo ""
echo "=== 安装依赖 ==="
cd "$FRONTEND_DIR"
npm install

echo ""
echo "=== 启动前端开发服务器 ==="
echo "访问地址: http://localhost:5173"
npm run dev
