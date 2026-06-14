# 探店路线规划系统 - 高德地图版本

> **版本**: V1.1.0  
> **更新日期**: 2026-06-09  
> **状态**: ✅ 高德地图功能已完整集成

---

## 🎉 最新更新

### V1.1.0 新功能（2026-06-09）
✅ **集成高德地图API**  
✅ 真实地理编码服务（地址转经纬度）  
✅ 真实路线规划服务（步行、公交、地铁）  
✅ 高德地图JS SDK集成  
✅ 实时地图展示和标注  
✅ 智能交通方式选择  

---

## 🌐 访问地址

**前端应用**: http://localhost:3000  
**后端API**: http://localhost:8080/api

---

## 📊 功能概览

### 核心功能

#### 1. 店铺管理 🏪
- 店铺增删改查
- 真实地理编码（地址自动转经纬度）
- 状态管理（未探店/已探店）
- 过期时间设置

#### 2. 清单管理 📋
- 创建探店清单
- 多店铺选择
- 状态管理（待执行/已完成/已取消）

#### 3. 路线规划 🗺️
**真实路线规划**（基于高德地图API）
- **时间优先方案**：地铁/快速公交优先
- **成本优先方案**：普通公交优先
- **均衡方案**：步行推荐（探店场景）

**智能交通选择**：
- ≤ 800米：步行
- 800-3000米：步行（非成本优先）
- > 3000米：公交/地铁

#### 4. 地图可视化 📍
**高德地图集成**：
- 3D地图展示
- 店铺位置标注
- 清单选择联动
- 实时位置显示

---

## 🚀 快速开始

### 1. 启动后端服务
```bash
cd backend
java -jar target/system-1.0.0.jar
```

### 2. 启动前端服务
```bash
cd frontend
npm run dev
```

### 3. 访问应用
打开浏览器访问：http://localhost:3000

---

## 🗂️ 项目结构

```
project2/
├── backend/              # 后端项目
│   ├── src/main/java/com/tandian/system/
│   │   ├── service/
│   │   │   ├── AmapService.java         # 高德地图服务 ✨
│   │   │   ├── ShopService.java
│   │   │   └── ShoppingListService.java
│   │   └── ...
│   └── src/main/resources/
│       └── application.yml              # 包含API Key配置
├── frontend/             # 前端项目
│   ├── src/
│   │   ├── components/
│   │   │   └── AMapComponent.vue        # 地图组件 ✨
│   │   ├── views/
│   │   │   ├── MapView.vue              # 地图首页
│   │   │   ├── ShopList.vue
│   │   │   └── ListDetail.vue
│   │   └── ...
│   └── index.html                       # 高德地图SDK引入
├── docs/                 # 文档
│   ├── 需求规格说明书.md
│   ├── 数据库设计文档.md
│   └── 高德地图集成完成报告.md
└── README.md
```

---

## 🔑 高德地图API配置

### 后端配置
```yaml
# backend/src/main/resources/application.yml
amap:
  api-key: 8d6e450b2c93a02b982ecd07c3dbb3d1
  enabled: true
```

### 前端配置
```html
<!-- frontend/index.html -->
<script src="https://webapi.amap.com/maps?v=2.0&key=YOUR_KEY"></script>
```

---

## 📡 API接口

### 地理编码（后端自动调用）
创建/编辑店铺时，系统自动调用地理编码API解析地址

### 路线规划
创建清单时自动生成3种路线方案，基于真实地图数据

### 店铺管理
- `GET /api/shops` - 获取店铺列表
- `POST /api/shops` - 创建店铺（自动地理编码）
- `PUT /api/shops/{id}` - 更新店铺
- `DELETE /api/shops/{id}` - 删除店铺

### 清单管理
- `GET /api/lists` - 获取清单列表
- `POST /api/lists` - 创建清单（自动路线规划）
- `GET /api/lists/{id}` - 获取清单详情（包含路线方案）
- `PUT /api/lists/{id}/complete` - 标记完成

---

## 🎨 技术栈

### 后端
- Spring Boot 2.7.18
- MyBatis-Plus 3.5.5
- MySQL 8.0
- **高德地图 Web服务API** ✨

### 前端
- Vue 3.4
- Vite 5.0
- Ant Design Vue 4.0
- **高德地图 JS API 2.0** ✨

---

## 🔧 开发说明

### 降级机制
当高德地图API不可用时，系统会自动降级：
- 地理编码：使用默认坐标
- 路线规划：使用模拟算法
- 地图展示：显示提示信息

### API配额
- 个人开发者：每日配额充足
- 建议：定期查看API调用量
- 优化：实现结果缓存（后期）

### 坐标系统
- 使用GCJ-02（国测局坐标）
- 与高德/谷歌地图兼容
- GPS坐标需转换

---

## 📚 文档目录

- [需求规格说明书](docs/需求规格说明书.md)
- [数据库设计文档](docs/数据库设计文档.md)
- [高德地图集成完成报告](docs/高德地图集成完成报告.md)
- [开发总结](docs/开发总结.md)

---

## ⚠️ 注意事项

1. **API Key安全**：生产环境建议使用域名白名单
2. **数据备份**：定期备份数据库
3. **性能监控**：关注API调用次数
4. **坐标系转换**：确保使用GCJ-02坐标

---

## 🚧 后续优化

### 短期
- 路线绘制优化
- 地图标记聚合
- 结果缓存机制

### 中期
- 实时路况集成
- POI搜索功能
- 导航功能

### 长期
- 离线地图支持
- 智能推荐算法
- 多用户系统

---

## 📞 技术支持

如有问题，请查看：
- [高德地图API文档](https://lbs.amap.com/api/)
- [常见问题](docs/开发总结.md)

---

**探店路线规划系统 V1.1.0 - 高德地图版本已就绪！** 🎉
