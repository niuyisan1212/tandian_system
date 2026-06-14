# 探店路线规划系统 - 部署指南

## 目录结构

```
project2/
├── docker-compose.yml          # Docker Compose 配置
├── deploy.sh                   # 服务端部署脚本
├── .github/workflows/
│   └── deploy.yml              # GitHub Actions 自动部署
├── frontend/
│   ├── Dockerfile              # 前端 Dockerfile
│   ├── nginx.conf              # Nginx 配置
│   └── .dockerignore           # Docker 忽略文件
└── backend/
    ├── Dockerfile              # 后端 Dockerfile
    └── .dockerignore           # Docker 忽略文件
```

---

## 一、GitHub 自动部署（推荐）

### 1.1 配置 GitHub Secrets

在 GitHub 仓库中配置以下 Secrets（Settings → Secrets and variables → Actions）：

| Secret 名称 | 说明 | 示例 |
|------------|------|------|
| `SERVER_HOST` | 服务器 IP 地址 | `47.108.59.58` |
| `SERVER_USER` | SSH 用户名 | `root` |
| `SERVER_PASSWORD` | SSH 密码 | `your_password` |
| `SERVER_PORT` | SSH 端口（可选） | `22` |
| `PROJECT_PATH` | 项目在服务器上的路径 | `/opt/tandian` |

### 1.2 服务器准备工作

```bash
# 1. 安装 Docker 和 Git
yum install -y git
curl -fsSL https://get.docker.com | sh
systemctl start docker
systemctl enable docker

# 2. 创建项目目录并克隆代码
mkdir -p /opt
cd /opt
git clone https://github.com/你的用户名/你的仓库名.git tandian
cd tandian

# 3. 首次手动部署（初始化数据库）
docker compose up -d
```

### 1.3 自动部署触发方式

**方式一：推送代码自动触发**
```bash
git add .
git commit -m "更新功能"
git push origin main
```

**方式二：手动触发**
1. 进入 GitHub 仓库
2. 点击 Actions 标签
3. 选择 "Auto Deploy" 工作流
4. 点击 "Run workflow"

### 1.4 查看部署日志

- GitHub Actions 页面：查看构建和部署日志
- 服务器日志：`docker compose logs -f`

---

## 二、手动部署

### 2.1 一键部署（推荐）

```bash
# 在服务器上执行
cd /opt/tandian

# 方式一：使用部署脚本
chmod +x deploy.sh
./deploy.sh

# 方式二：使用 docker compose
docker compose up -d --build
```

### 2.2 单独构建部署

```bash
# 构建后端
cd backend
docker build -t tandian-backend:latest .

# 构建前端
cd frontend
docker build -t tandian-frontend:latest .
```

---

## 三、配置说明

### 3.1 后端配置

修改 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/tandian_system
    username: root
    password: niuhaoqing1212

amap:
  api-key: fc683a6ac5b6a586336aeb843514a4f4
  security-key: 55dff51094e9f5ddbcf1153a6cebf203
```

### 3.2 端口映射

| 服务 | 容器端口 | 宿主机端口 |
|------|----------|------------|
| 前端 | 80 | 80 |
| 后端 | 8080 | 8080 |
| MySQL | 3306 | 3306 |

---

## 四、常用运维命令

```bash
# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f
docker compose logs -f backend
docker compose logs -f frontend

# 重启服务
docker compose restart
docker compose restart backend

# 停止服务
docker compose down

# 更新部署
git pull
docker compose up -d --build

# 进入容器调试
docker compose exec backend sh
docker compose exec mysql mysql -uroot -p
```

---

## 五、数据库迁移

如果已有数据库需要添加类别字段：

```bash
# 连接数据库
docker compose exec mysql mysql -uroot -pniuhaoqing1212

# 执行迁移
ALTER TABLE tandian_system.shop ADD COLUMN category VARCHAR(50) DEFAULT '美食' COMMENT '店铺类别' AFTER name;
ALTER TABLE tandian_system.shop ADD INDEX idx_category (category);
```

---

## 六、访问地址

部署成功后：

| 服务 | 地址 |
|------|------|
| 前端 | `http://服务器IP` |
| 后端 API | `http://服务器IP:8080/api` |

---

## 七、生产环境注意事项

1. **修改默认密码**：修改 MySQL root 密码
2. **配置 HTTPS**：使用 SSL 证书
3. **限制端口**：移除 MySQL 外部端口映射
4. **数据备份**：定期备份 MySQL 数据卷
5. **日志管理**：配置日志轮转
