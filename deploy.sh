#!/bin/bash
# ============================================
# 探店路线规划系统 - 服务端部署脚本
# 用途：在服务器上执行自动更新和部署
# ============================================

set -e  # 遇到错误立即退出

# 配置变量（根据实际情况修改）
PROJECT_DIR="/opt/tandian"
GIT_BRANCH="main"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  探店路线规划系统 - 自动部署${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 记录开始时间
START_TIME=$(date +%s)
echo -e "${YELLOW}开始时间: $(date '+%Y-%m-%d %H:%M:%S')${NC}"

# 进入项目目录
cd $PROJECT_DIR || {
    echo -e "${RED}错误: 项目目录不存在 $PROJECT_DIR${NC}"
    exit 1
}

# 1. 拉取最新代码
echo ""
echo -e "${YELLOW}[1/6] 拉取最新代码...${NC}"
git fetch origin
git reset --hard origin/$GIT_BRANCH
git pull origin $GIT_BRANCH

# 2. 停止旧容器
echo ""
echo -e "${YELLOW}[2/6] 停止旧容器...${NC}"
docker compose down

# 3. 清理旧镜像（可选）
echo ""
echo -e "${YELLOW}[3/6] 清理旧镜像...${NC}"
docker image prune -f

# 4. 构建新镜像
echo ""
echo -e "${YELLOW}[4/6] 构建新镜像...${NC}"
docker compose build --no-cache

# 5. 启动服务
echo ""
echo -e "${YELLOW}[5/6] 启动服务...${NC}"
docker compose up -d

# 6. 等待并检查服务状态
echo ""
echo -e "${YELLOW}[6/6] 检查服务状态...${NC}"
sleep 30

# 显示容器状态
echo ""
docker compose ps

# 检查服务是否正常运行
FRONTEND_STATUS=$(docker inspect -f '{{.State.Status}}' tandian-frontend 2>/dev/null || echo "not found")
BACKEND_STATUS=$(docker inspect -f '{{.State.Status}}' tandian-backend 2>/dev/null || echo "not found")
MYSQL_STATUS=$(docker inspect -f '{{.State.Status}}' tandian-mysql 2>/dev/null || echo "not found")

echo ""
echo -e "${GREEN}服务状态:${NC}"
echo "  前端 (frontend): $FRONTEND_STATUS"
echo "  后端 (backend):  $BACKEND_STATUS"
echo "  数据库 (mysql):  $MYSQL_STATUS"

# 计算耗时
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${GREEN}  耗时: ${DURATION} 秒${NC}"
echo -e "${GREEN}========================================${NC}"

# 显示访问地址
SERVER_IP=$(curl -s ifconfig.me 2>/dev/null || echo "localhost")
echo ""
echo -e "${GREEN}访问地址:${NC}"
echo "  前端: http://$SERVER_IP"
echo "  后端: http://$SERVER_IP:8080/api"
echo ""

# 如果有任何服务未运行，返回错误码
if [ "$FRONTEND_STATUS" != "running" ] || [ "$BACKEND_STATUS" != "running" ] || [ "$MYSQL_STATUS" != "running" ]; then
    echo -e "${RED}警告: 部分服务未正常运行，请检查日志${NC}"
    echo ""
    echo "查看日志命令:"
    echo "  docker compose logs frontend"
    echo "  docker compose logs backend"
    echo "  docker compose logs mysql"
    exit 1
fi
