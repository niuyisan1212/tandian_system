# ============================================
# 探店路线规划系统 - 前端 Dockerfile
# 多阶段构建：构建阶段 + 生产阶段
# ============================================

# 阶段1: 构建阶段
FROM node:18-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制 package.json 和 package-lock.json（如果存在）
COPY package*.json ./

# 安装依赖（使用国内镜像加速）
RUN npm install --registry=https://registry.npmmirror.com

# 复制源代码
COPY . .

# 构建生产版本（限制 Node 内存为 512M）
ENV NODE_OPTIONS="--max-old-space-size=512"
RUN npm run build

# 阶段2: 生产阶段
FROM nginx:alpine

# 作者信息
LABEL maintainer="nhq"

# 设置时区
ENV TimeZone=Asia/Shanghai

# 复制自定义 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制构建产物到 nginx 目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q --spider http://localhost/ || exit 1

# 启动 nginx
CMD ["nginx", "-g", "daemon off;"]
