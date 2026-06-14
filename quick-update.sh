#!/bin/bash
# ============================================
# 探店路线规划系统 - 快速更新脚本
# 
# 用于快速拉取代码并重启服务
# 适合配合 GitHub Webhook 或定时任务使用
# ============================================

cd /opt/tandian

echo "===== 开始更新 $(date '+%Y-%m-%d %H:%M:%S') ====="

# 拉取代码
git fetch origin
LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse origin/main)

if [ "$LOCAL" = "$REMOTE" ]; then
    echo "代码已是最新，无需更新"
    exit 0
fi

echo "发现更新，开始部署..."
git reset --hard origin/main
git pull origin main

# 重启服务
docker compose down
docker compose up -d --build

# 等待启动
sleep 20

# 检查状态
docker compose ps

echo "===== 部署完成 ====="
