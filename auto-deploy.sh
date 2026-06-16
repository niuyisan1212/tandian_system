#!/bin/bash
# ============================================
# 探店路线规划系统 - 服务端自动部署脚本
# 项目前缀: td (tandian)
# 部署流程：备份 → 拉代码 → 构建 → 恢复
# ============================================

set -e  # 遇到错误立即退出

# ==================== 配置区域 ====================
# 项目目录
PROJECT_DIR="/home/website/tandian"

# Git 分支
GIT_BRANCH="main"

# 是否强制拉取（覆盖本地修改）
FORCE_PULL=true

# 部署后是否清理旧镜像
CLEANUP_IMAGES=true

# 健康检查等待时间（秒）
HEALTH_CHECK_WAIT=30

# 保留最近几个备份
KEEP_BACKUPS=5
# ================================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 日志函数
log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step()  { echo -e "${BLUE}[STEP]${NC} $1"; }

print_separator() { echo -e "${GREEN}================================================${NC}"; }

# 主流程
main() {
    print_separator
    echo -e "${GREEN}  探店路线规划系统 - 自动部署${NC}"
    echo -e "${GREEN}  时间: $(date '+%Y-%m-%d %H:%M:%S')${NC}"
    print_separator
    echo ""

    START_TIME=$(date +%s)

    if [ ! -d "$PROJECT_DIR" ]; then
        log_error "项目目录不存在: $PROJECT_DIR"
        exit 1
    fi

    cd "$PROJECT_DIR"
    log_info "进入项目目录: $PROJECT_DIR"

    # ==================== 步骤1: 备份数据库 ====================
    print_separator
    log_step "步骤 1/6: 备份数据库"
    
    BACKUP_DIR="./backups"
    mkdir -p $BACKUP_DIR
    BACKUP_FILE="$BACKUP_DIR/tandian_$(date '+%Y%m%d_%H%M%S').sql"
    
    if docker ps --format '{{.Names}}' | grep -q '^td-mysql$'; then
        log_info "MySQL 容器运行中，开始备份..."
        docker exec td-mysql mysqldump -uroot -pniuhaoqing1212 tandian_system > "$BACKUP_FILE" 2>/dev/null
        
        if [ -s "$BACKUP_FILE" ]; then
            BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
            log_info "备份成功: $BACKUP_FILE ($BACKUP_SIZE)"
        else
            log_warn "备份文件为空，可能是首次部署"
            rm -f "$BACKUP_FILE"
            BACKUP_FILE=""
        fi
    else
        log_info "MySQL 容器未运行，跳过备份"
        BACKUP_FILE=""
    fi
    
    # 清理旧备份，只保留最近 N 个
    cd $BACKUP_DIR && ls -t tandian_*.sql 2>/dev/null | tail -n +$(($KEEP_BACKUPS + 1)) | xargs rm -f 2>/dev/null; cd -
    log_info "保留最近 $KEEP_BACKUPS 个备份"

    # ==================== 步骤2: 拉取代码 ====================
    print_separator
    log_step "步骤 2/6: 拉取最新代码"
    
    if [ "$FORCE_PULL" = true ]; then
        git reset --hard HEAD
    fi
    
    git fetch origin
    
    LOCAL_HASH=$(git rev-parse HEAD)
    REMOTE_HASH=$(git rev-parse origin/$GIT_BRANCH)
    
    if [ "$LOCAL_HASH" = "$REMOTE_HASH" ]; then
        log_info "代码已是最新，无需更新"
        SKIP_BUILD=true
    else
        log_info "发现新版本: $LOCAL_HASH -> $REMOTE_HASH"
        git pull origin $GIT_BRANCH
        SKIP_BUILD=false
    fi

    # ==================== 步骤3: 构建并启动 ====================
    print_separator
    log_step "步骤 3/6: 构建并启动服务"
    
    if [ "$SKIP_BUILD" = true ]; then
        log_info "代码未更新，跳过构建"
    else
        log_info "构建并启动容器..."
        docker compose up -d --build
    fi

    # ==================== 步骤4: 等待并恢复数据 ====================
    print_separator
    log_step "步骤 4/6: 等待服务启动并检查数据"
    
    log_info "等待服务启动（${HEALTH_CHECK_WAIT}秒）..."
    sleep $HEALTH_CHECK_WAIT
    
    # 如果有备份，检查数据库是否为空
    if [ -n "$BACKUP_FILE" ] && [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
        TABLE_COUNT=$(docker exec td-mysql mysql -uroot -pniuhaoqing1212 -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='tandian_system'" -s -N 2>/dev/null || echo "0")
        
        if [ "$TABLE_COUNT" = "0" ] || [ -z "$TABLE_COUNT" ]; then
            log_warn "数据库为空，从备份恢复..."
            docker exec -i td-mysql mysql -uroot -pniuhaoqing1212 tandian_system < "$BACKUP_FILE" 2>/dev/null
            log_info "数据恢复完成"
        else
            log_info "数据库已有数据（$TABLE_COUNT 张表），无需恢复"
        fi
    fi

    # ==================== 步骤5: 健康检查 ====================
    print_separator
    log_step "步骤 5/6: 健康检查"
    
    check_service() {
        local container=$1
        local max_retries=3
        local retry=0
        
        while [ $retry -lt $max_retries ]; do
            STATUS=$(docker inspect -f '{{.State.Status}}' "$container" 2>/dev/null || echo "not found")
            if [ "$STATUS" = "running" ]; then
                log_info "$container: running"
                return 0
            else
                retry=$((retry + 1))
                [ $retry -lt $max_retries ] && log_warn "$container: $STATUS，重试 ($retry/$max_retries)..." && sleep 10
            fi
        done
        
        log_error "$container: $STATUS 启动失败"
        return 1
    }
    
    ALL_HEALTHY=true
    echo ""
    check_service "td-mysql"    || ALL_HEALTHY=false
    check_service "td-backend"  || ALL_HEALTHY=false
    check_service "td-frontend" || ALL_HEALTHY=false
    echo ""

    # ==================== 步骤6: 清理 ====================
    print_separator
    log_step "步骤 6/6: 清理旧镜像"
    
    if [ "$CLEANUP_IMAGES" = true ]; then
        docker image prune -f
        log_info "清理完成"
    fi

    # ==================== 结果 ====================
    print_separator
    
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    
    if [ "$ALL_HEALTHY" = true ]; then
        echo -e "${GREEN}  部署成功！耗时: ${DURATION} 秒${NC}"
        print_separator
        echo ""
        docker compose ps
        echo ""
    else
        echo -e "${RED}  部署失败！${NC}"
        print_separator
        echo ""
        echo "查看日志:"
        echo "  docker compose logs td-mysql"
        echo "  docker compose logs td-backend"
        echo "  docker compose logs td-frontend"
        echo ""
        echo "恢复数据:"
        echo "  docker exec -i td-mysql mysql -uroot -pniuhaoqing1212 tandian_system < $BACKUP_FILE"
        exit 1
    fi
}

main "$@"
