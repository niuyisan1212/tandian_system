# 探店路线规划系统 - 项目启动说明

## 项目已成功启动！

### 访问地址

**后端服务**: http://localhost:8080/api  
**前端应用**: http://localhost:3000

### 项目结构

```
project2/
├── backend/              # 后端项目（Spring Boot 2.7 + MyBatis-Plus）
│   ├── src/             # 源代码
│   ├── target/          # 编译输出
│   ├── pom.xml          # Maven配置
│   └── README.md        # 后端文档
├── frontend/            # 前端项目（Vue 3 + Vite + Ant Design Vue）
│   ├── src/             # 源代码
│   ├── package.json     # NPM配置
│   └── README.md        # 前端文档
├── docs/                # 项目文档
│   ├── 需求规格说明书.md
│   └── 数据库设计文档.md
├── logs/                # 日志目录
└── README.md            # 本文档
```

---

## 快速开始

### 1. 启动后端服务

#### 方式一：使用打包好的JAR文件
```bash
cd backend
java -jar target/system-1.0.0.jar
```

#### 方式二：使用Maven运行
```bash
cd backend
mvn spring-boot:run
```

后端服务将在 http://localhost:8080/api 启动

### 2. 启动前端服务

#### 首次运行需要安装依赖
```bash
cd frontend
npm install
```

#### 启动开发服务器
```bash
cd frontend
npm run dev
```

前端应用将在 http://localhost:3000 启动

---

## 数据库配置

### 数据库信息
- **数据库名**: tandian_system
- **地址**: localhost:3306
- **用户名**: root
- **密码**: niuhaoqing1212

### 初始化数据库
```bash
mysql -u root -p < backend/sql/init.sql
```

数据库已初始化完成，包含：
- 10个测试店铺
- 2个测试清单
- 完整的表结构

---

## 功能说明

### 1. 地图首页 (/)
- 展示所有待探店店铺位置
- 统计数据（总店铺数、待探店、已探店、完成率）
- 清单选择器
- 地图可视化

### 2. 店铺管理 (/shops)
- 店铺增删改查
- 分页查询与筛选
- 按名称/地址搜索
- 状态管理（未探店/已探店）
- 过期时间设置

### 3. 探店清单 (/lists)
- 创建探店清单
- 选择待探店铺
- 清单状态管理（待执行/已完成/已取消）
- 查看清单详情

### 4. 清单详情 (/lists/:id)
- 查看清单基本信息
- 多路线方案对比（时间优先/成本优先/均衡）
- 路线分段指引
- 标记完成/取消

---

## API接口

### 店铺相关
- `GET /api/shops` - 分页查询店铺列表
- `GET /api/shops/{id}` - 获取店铺详情
- `POST /api/shops` - 创建店铺
- `PUT /api/shops/{id}` - 更新店铺
- `DELETE /api/shops/{id}` - 删除店铺
- `GET /api/shops/valid` - 获取有效待探店店铺
- `PUT /api/shops/{id}/visit` - 标记店铺为已探店

### 清单相关
- `GET /api/lists` - 分页查询清单列表
- `GET /api/lists/{id}` - 获取清单详情
- `POST /api/lists` - 创建清单
- `PUT /api/lists/{id}` - 更新清单
- `DELETE /api/lists/{id}` - 删除清单
- `PUT /api/lists/{id}/complete` - 标记清单为已完成
- `PUT /api/lists/{id}/cancel` - 标记清单为已取消

### 统计相关
- `GET /api/statistics/overview` - 获取统计数据

---

## 测试账号

系统无需登录，直接访问即可使用所有功能。

---

## 常见问题

### 1. 后端启动失败
- 检查数据库是否启动
- 检查数据库连接配置是否正确
- 检查端口8080是否被占用

### 2. 前端无法访问后端接口
- 确认后端服务已启动
- 检查浏览器控制台是否有跨域错误
- 确认API代理配置正确

### 3. 地图不显示
- 首版使用模拟数据展示地图
- 实际部署需配置高德地图API Key

---

## 开发说明

### 技术栈
- **后端**: Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 + MySQL 8.0
- **前端**: Vue 3.4 + Vite 5.0 + Ant Design Vue 4.0
- **工具**: Maven 3.6+、Node.js 18+

### 开发环境
- Java 1.8
- Maven 3.6+
- Node.js 18+
- MySQL 8.0

### 编译打包

**后端打包**
```bash
cd backend
mvn clean package
```

**前端打包**
```bash
cd frontend
npm run build
```

---

## 部署说明

### 生产环境部署

**1. 后端部署**
```bash
# 编译打包
cd backend
mvn clean package

# 运行JAR包
java -jar target/system-1.0.0.jar

# 或使用systemd服务
sudo systemctl start tandian-system
```

**2. 前端部署**
```bash
# 编译打包
cd frontend
npm run build

# 将dist目录部署到Nginx或Apache
```

**3. Nginx配置示例**
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        root /path/to/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 注意事项

1. **数据库密码**: 配置文件中密码明文存储，生产环境建议使用环境变量
2. **API Key管理**: 首版使用模拟数据，对接真实API需配置高德地图Key
3. **数据备份**: 系统不提供自动备份，需定期备份数据库
4. **安全性**: 首版无登录系统，生产环境建议添加认证机制

---

## 项目特点

✅ **前后端分离**: 采用主流的前后端分离架构  
✅ **响应式设计**: 支持PC和移动端访问  
✅ **现代化UI**: 使用Ant Design Vue组件库  
✅ **RESTful API**: 标准的RESTful接口设计  
✅ **代码规范**: 注释完善，结构清晰  
✅ **可扩展性**: 预留外部API接口，便于后期扩展  

---

## 后续优化方向

1. **对接高德地图API**: 实现真实的地理编码和路线规划
2. **批量导入导出**: 实现Excel导入导出功能
3. **智能推荐**: 基于历史数据优化路线推荐
4. **实时路况**: 集成实时交通信息
5. **用户系统**: 添加登录和多用户支持
6. **移动端优化**: 进一步优化移动端体验

---

## 联系方式

如有问题或建议，请联系开发团队。

---

**祝您使用愉快！**
