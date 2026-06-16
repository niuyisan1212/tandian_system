#!/bin/bash
# ============================================
# 探店路线规划系统 - 快速更新脚本
# 部署流程：备份 → 拉代码 → 构建 → 恢复
# ============================================

cd /home/website/tandian

echo "===== 开始更新 $(date '+%Y-%m-%d %H:%M:%S') ====="

# ==================== 备份数据库 ====================
BACKUP_DIR="./backups"
mkdir -p $BACKUP_DIR
BACKUP_FILE="$BACKUP_DIR/tandian_$(date '+%Y%m%d_%H%M%S').sql"

if docker ps --format '{{.Names}}' | grep -q '^td-mysql$'; then
    echo "备份数据库..."
    docker exec td-mysql mysqldump -uroot -pniuhaoqing1212 tandian_system > "$BACKUP_FILE" 2>/dev/null
    if [ -s "$BACKUP_FILE" ]; then
        echo "备份成功: $BACKUP_FILE"
    else
        echo "备份为空，可能首次部署"
        rm -f "$BACKUP_FILE"
        BACKUP_FILE=""
    fi
else
    echo "MySQL 未运行，跳过备份"
    BACKUP_FILE=""
fi

# 只保留最近 5 个备份
cd $BACKUP_DIR && ls -t tandian_*.sql 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null; cd -

# ==================== 拉取代码 ====================
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

# ==================== 构建并启动 ====================
docker compose up -d --build

# ==================== 等待并恢复数据 ====================
sleep 30

if [ -n "$BACKUP_FILE" ] && [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
    TABLE_COUNT=$(docker exec td-mysql mysql -uroot -pniuhaoqing1212 -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='tandian_system'" -s -N 2>/dev/null || echo "0")
    if [ "$TABLE_COUNT" = "0" ] || [ -z "$TABLE_COUNT" ]; then
        echo "数据库为空，从备份恢复..."
        docker exec -i td-mysql mysql -uroot -pniuhaoqing1212 tandian_system < "$BACKUP_FILE" 2>/dev/null
        echo "数据恢复完成"
    else
        echo "数据库已有数据，无需恢复"
    fi
fi

# ==================== 检查状态 ====================
docker compose ps

echo "===== 部署完成 ====="
