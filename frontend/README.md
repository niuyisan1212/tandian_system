# 探店路线规划系统 - 前端

> 基于Vue 3 + Vite + Ant Design Vue构建的探店路线规划系统前端应用

## 项目简介

探店路线规划系统前端提供直观的用户界面，支持店铺管理、清单管理、地图可视化、路线规划等功能，采用响应式设计，支持PC和移动端访问。

## 技术栈

- **框架**: Vue 3.4
- **构建工具**: Vite 5.0
- **UI组件库**: Ant Design Vue 4.0
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **HTTP客户端**: Axios
- **日期处理**: Day.js

## 项目结构

```
frontend/
├── public/                  # 静态资源
├── src/
│   ├── api/                # API接口封装
│   ├── assets/             # 资源文件
│   ├── components/         # 公共组件
│   ├── router/             # 路由配置
│   ├── stores/             # 状态管理
│   ├── views/              # 页面组件
│   ├── App.vue             # 根组件
│   └── main.js             # 入口文件
├── index.html              # HTML模板
├── vite.config.js          # Vite配置
└── package.json            # 项目配置
```

## 功能模块

### 地图首页
- 展示所有待探店店铺位置
- 清单选择与联动
- 统计数据展示
- 地图可视化

### 店铺管理
- 店铺CRUD操作
- 分页查询与筛选
- 状态管理（未探店/已探店）
- 过期时间设置

### 探店清单
- 创建探店清单
- 选择待探店铺
- 自动生成路线方案
- 清单状态管理（待执行/已完成/已取消）

### 清单详情
- 查看清单信息
- 多方案对比（时间优先/成本优先/均衡）
- 路线分段指引
- 地图路线展示

## 快速开始

### 前置要求

- Node.js 18+
- npm 9+ 或 yarn 1.22+

### 安装依赖

```bash
npm install
# 或
yarn install
```

### 开发模式

```bash
npm run dev
# 或
yarn dev
```

访问 http://localhost:3000

### 生产构建

```bash
npm run build
# 或
yarn build
```

### 预览构建

```bash
npm run preview
# 或
yarn preview
```

## 配置说明

### API代理配置

开发环境下，API请求会自动代理到后端服务：

```javascript
// vite.config.js
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 路径别名

```javascript
// vite.config.js
resolve: {
  alias: {
    '@': resolve(__dirname, 'src')
  }
}
```

使用示例：
```javascript
import { getShopList } from '@/api'
import MyComponent from '@/components/MyComponent.vue'
```

## 页面路由

| 路由 | 页面 | 说明 |
|------|------|------|
| `/` | MapView | 地图首页，展示所有待探店位置 |
| `/shops` | ShopList | 店铺管理页面 |
| `/lists` | ListManager | 探店清单列表 |
| `/lists/:id` | ListDetail | 清单详情页 |

## API接口

### 店铺相关
- `getShopList(params)` - 获取店铺列表（分页）
- `getValidShops()` - 获取有效待探店店铺
- `getShopById(id)` - 获取店铺详情
- `createShop(data)` - 创建店铺
- `updateShop(id, data)` - 更新店铺
- `deleteShop(id)` - 删除店铺

### 清单相关
- `getListPage(params)` - 获取清单列表（分页）
- `getListById(id)` - 获取清单详情
- `createList(data)` - 创建清单
- `updateList(id, data)` - 更新清单
- `deleteList(id)` - 删除清单
- `markListCompleted(id)` - 标记清单为已完成
- `markListCancelled(id)` - 标记清单为已取消

### 统计相关
- `getStatistics()` - 获取统计数据

## 地图集成

### 高德地图API（预留）

本项目预留了高德地图API集成接口，实际使用时需要：

1. 申请高德地图开发者Key
2. 在HTML中引入高德地图JS SDK
3. 配置API Key

```html
<script src="https://webapi.amap.com/maps?v=2.0&key=YOUR_KEY"></script>
```

首版使用模拟数据展示，便于快速开发和测试。

## UI设计规范

### 设计原则
- 简洁、清新、大气
- 响应式布局
- 一致的交互体验

### 组件库
使用Ant Design Vue 4.0，遵循其设计规范

### 主题定制
可在`src/assets/main.css`中自定义样式

## 注意事项

1. **地图API Key**: 实际部署时需配置真实的高德地图API Key
2. **后端地址**: 生产环境需修改API代理配置
3. **浏览器兼容**: 支持现代浏览器（Chrome、Firefox、Safari、Edge）

## 开发建议

### 代码规范
- 使用Composition API
- 组件命名使用PascalCase
- 文件命名使用kebab-case
- 注释清晰明了

### 最佳实践
- 组件复用，避免重复代码
- 合理使用Pinia进行状态管理
- API请求统一错误处理
- 路由懒加载优化性能

## 构建优化

### 生产构建优化
- 代码分割
- 资源压缩
- Tree shaking

### 性能优化
- 路由懒加载
- 图片懒加载
- 虚拟滚动（大数据列表）

## 版本历史

- v1.0.0 (2026-06-08)
  - 完成核心页面：地图首页、店铺管理、清单管理、清单详情
  - 实现前后端API对接
  - 使用模拟数据展示地图

## 许可证

MIT License

## 联系方式

如有问题，请提交Issue或联系开发团队。
