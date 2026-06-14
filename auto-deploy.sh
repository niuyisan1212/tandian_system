#!/bin/bash
# ============================================
# 探店路线规划系统 - 服务端自动部署脚本
# 
# 使用方法：
#   chmod +x auto-deploy.sh
#   ./auto-deploy.sh
# 
# 建议：可以配置 crontab 定时检查更新
# ============================================

set -e  # 遇到错误立即退出

# ==================== 配置区域 ====================
# 项目目录（根据实际情况修改）
PROJECT_DIR="/opt/tandian"

# Git 分支
GIT_BRANCH="master"

# 是否强制拉取（覆盖本地修改）
FORCE_PULL=true

# 部署后是否清理旧镜像
CLEANUP_IMAGES=true

# 健康检查等待时间（秒）
HEALTH_CHECK_WAIT=30

# 最大等待时间（秒）
MAX_WAIT=120
# ================================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'  # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 分隔线
print_separator() {
    echo -e "${GREEN}================================================${NC}"
}

# 获取当前时间
get_timestamp() {
    date '+%Y-%m-%d %H:%M:%S'
}

# 主流程
main() {
    print_separator
    echo -e "${GREEN}  探店路线规划系统 - 自动部署${NC}"
    echo -e "${GREEN}  时间: $(get_timestamp)${NC}"
    print_separator
    echo ""

    # 记录开始时间
    START_TIME=$(date +%s)

    # 检查项目目录
    if [ ! -d "$PROJECT_DIR" ]; then
        log_error "项目目录不存在: $PROJECT_DIR"
        log_info "请先克隆项目: git clone <仓库地址> $PROJECT_DIR"
        exit 1
    fi

    cd "$PROJECT_DIR"
    log_info "进入项目目录: $PROJECT_DIR"

    # ==================== 步骤1: 检查 Git 状态 ====================
    print_separator
    log_step "步骤 1/7: 检查 Git 状态"
    
    # 检查是否有未提交的修改
    if [ "$FORCE_PULL" = true ]; then
        git reset --hard HEAD
        log_info "已重置本地修改"
    else
        if [ -n "$(git status --porcelain)" ]; then
            log_warn "存在未提交的修改，跳过更新"
            log_warn "如需强制更新，请设置 FORCE_PULL=true"
        fi
    fi

    # ==================== 步骤2: 拉取最新代码 ====================
    print_separator
    log_step "步骤 2/7: 拉取最新代码"
    
    log_info "从远程仓库获取更新..."
    git fetch origin
    
    LOCAL_HASH=$(git rev-parse HEAD)
    REMOTE_HASH=$(git rev-parse origin/$GIT_BRANCH)
    
    if [ "$LOCAL_HASH" = "$REMOTE_HASH" ]; then
        log_info "代码已是最新，无需更新"
        SKIP_BUILD=true
    else
        log_info "发现新版本，正在拉取..."
        log_info "本地: $LOCAL_HASH"
        log_info "远程: $REMOTE_HASH"
        git pull origin $GIT_BRANCH
        SKIP_BUILD=false
    fi

    # ==================== 步骤3: 停止旧容器 ====================
    print_separator
    log_step "步骤 3/7: 停止旧容器"
    
    if docker compose ps -q 2>/dev/null | grep -q .; then
        log_info "停止运行中的容器..."
        docker compose down
        log_info "容器已停止"
    else
        log_info "没有运行中的容器"
    fi

    # ==================== 步骤4: 构建新镜像 ====================
    print_separator
    log_step "步骤 4/7: 构建新镜像"
    
    if [ "$SKIP_BUILD" = true ]; then
        log_info "代码未更新，跳过构建"
    else
        log_info "开始构建 Docker 镜像（首次构建可能需要几分钟）..."
        docker compose build --no-cache
        log_info "镜像构建完成"
    fi

    # ==================== 步骤5: 启动服务 ====================
    print_separator
    log_step "步骤 5/7: 启动服务"
    
    log_info "启动 Docker 容器..."
    docker compose up -d
    log_info "容器启动命令已执行"

    # ==================== 步骤6: 健康检查 ====================
    print_separator
    log_step "步骤 6/7: 健康检查"
    
    log_info "等待服务启动（${HEALTH_CHECK_WAIT}秒）..."
    sleep $HEALTH_CHECK_WAIT
    
    # 检查各服务状态
    check_service() {
        local service=$1
        local container="tandian-$service"
        local max_retries=3
        local retry=0
        
        while [ $retry -lt $max_retries ]; do
            STATUS=$(docker inspect -f '{{.State.Status}}' "$container" 2>/dev/null || echo "not found")
            
            if [ "$STATUS" = "running" ]; then
                log_info "$service: $STATUS ✓"
                return 0
            else
                retry=$((retry + 1))
                if [ $retry -lt $max_retries ]; then
                    log_warn "$service: $STATUS，等待重试 ($retry/$max_retries)..."
                    sleep 10
                fi
            fi
        done
        
        log_error "$service: $STATUS ✗"
        return 1
    }
    
    # 检查所有服务
    ALL_HEALTHY=true
    
    echo ""
    check_service "mysql" || ALL_HEALTHY=false
    check_service "backend" || ALL_HEALTHY=false
    check_service "frontend" || ALL_HEALTHY=false
    echo ""

    # ==================== 步骤7: 清理工作 ====================
    print_separator
    log_step "步骤 7/7: 清理工作"
    
    if [ "$CLEANUP_IMAGES" = true ]; then
        log_info "清理未使用的 Docker 镜像..."
        docker image prune -f
        log_info "清理完成"
    fi

    # ==================== 部署结果 ====================
    print_separator
    
    # 计算耗时
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    
    if [ "$ALL_HEALTHY" = true ]; then
        echo -e "${GREEN}  ✓ 部署成功！${NC}"
        echo -e "${GREEN}  耗时: ${DURATION} 秒${NC}"
        print_separator
        echo ""
        
        # 获取服务器 IP
        SERVER_IP=$(curl -s --connect-timeout 2 ifconfig.me 2>/dev/null || echo "localhost")
        
        echo -e "${GREEN}访问地址:${NC}"
        echo -e "  前端:  ${BLUE}http://$SERVER_IP${NC}"
        echo -e "  后端:  ${BLUE}http://$SERVER_IP:8080/api${NC}"
        echo ""
        
        # 显示容器状态
        echo -e "${GREEN}容器状态:${NC}"
        docker compose ps
        echo ""
        
    else
        echo -e "${RED}  ✗ 部署失败！部分服务未正常启动${NC}"
        print_separator
        echo ""
        
        # 显示错误日志
        echo -e "${YELLOW}查看错误日志:${NC}"
        echo "  docker compose logs mysql"
        echo "  docker compose logs backend"
        echo "  docker compose logs frontend"
        echo ""
        
        exit 1
    fi
}

# 执行主流程
main "$@"
