# 探店路线规划系统 - 后端

> 基于Spring Boot 3 + MyBatis-Plus构建的探店路线规划系统后端服务

## 项目简介

探店路线规划系统后端提供店铺管理、清单管理、路线规划等核心功能，采用RESTful API设计，支持前后端分离架构。

## 技术栈

- **框架**: Spring Boot 3.2.0
- **ORM**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8.0
- **构建工具**: Maven
- **日志**: SLF4J + Logback
- **工具库**: Hutool、Fastjson

## 项目结构

```
backend/
├── src/main/java/com/tandian/system/
│   ├── controller/          # 控制器层
│   ├── service/             # 业务逻辑层
│   ├── mapper/              # 数据访问层
│   ├── entity/              # 实体类
│   ├── dto/                 # 数据传输对象
│   ├── vo/                  # 视图对象
│   ├── config/              # 配置类
│   └── exception/           # 异常处理
├── src/main/resources/
│   └── application.yml      # 配置文件
├── sql/
│   └── init.sql            # 数据库初始化脚本
└── pom.xml                 # Maven配置
```

## 功能模块

### 店铺管理
- 店铺CRUD操作
- 店铺状态管理（未探店/已探店）
- 过期时间设置
- 分页查询与筛选

### 清单管理
- 创建探店清单
- 清单状态管理（待执行/已完成/已取消）
- 清单完成自动更新店铺状态
- 多清单状态同步

### 路线规划
- 自动生成多种路线方案（时间优先/成本优先/均衡）
- 基于TSP算法优化访问顺序
- 支持步行、公交、地铁等交通方式
- 分段路线指引

### 统计分析
- 店铺探店统计
- 完成率计算
- 历史数据分析

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 数据库配置

1. 创建数据库并导入初始数据：
```bash
mysql -u root -p < sql/init.sql
```

2. 修改数据库配置（如需要）：
```yaml
# src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tandian_system
    username: root
    password: your_password
```

### 启动项目

```bash
# 编译项目
mvn clean package

# 运行项目
java -jar target/system-1.0.0.jar

# 或使用Maven直接运行
mvn spring-boot:run
```

服务启动后访问：http://localhost:8080/api

## API文档

### 店铺相关

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/shops | 分页查询店铺列表 |
| GET | /api/shops/{id} | 获取店铺详情 |
| POST | /api/shops | 创建店铺 |
| PUT | /api/shops/{id} | 更新店铺 |
| DELETE | /api/shops/{id} | 删除店铺 |
| GET | /api/shops/valid | 获取有效待探店店铺 |
| PUT | /api/shops/{id}/visit | 标记店铺为已探店 |

### 清单相关

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/lists | 分页查询清单列表 |
| GET | /api/lists/{id} | 获取清单详情 |
| POST | /api/lists | 创建清单 |
| PUT | /api/lists/{id} | 更新清单 |
| DELETE | /api/lists/{id} | 删除清单 |
| PUT | /api/lists/{id}/complete | 标记清单为已完成 |
| PUT | /api/lists/{id}/cancel | 标记清单为已取消 |
| POST | /api/lists/{id}/replan | 重新规划路线 |
| PUT | /api/lists/{id}/select-plan | 选择路线方案 |

### 统计相关

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/statistics/overview | 获取统计数据 |

## 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tandian_system
    username: root
    password: your_password
```

### 日志配置
```yaml
logging:
  level:
    com.tandian.system: DEBUG
  file:
    name: logs/tandian-system.log
```

### 高德地图API（预留）
```yaml
amap:
  api-key: YOUR_AMAP_KEY
  enabled: false  # 首版使用模拟数据
```

## 核心算法

### 路线规划算法
1. **TSP问题求解**：使用最近邻算法生成初始路径
2. **距离计算**：简化欧几里得距离计算
3. **交通方式决策**：根据距离选择步行/公交/地铁
4. **多方案生成**：时间优先、成本优先、均衡方案

## 扩展性设计

### 地理编码接口
首版使用模拟数据，预留接口对接真实地理编码API：
```java
// 后续可对接高德地图API
public interface GeocodingService {
    Location geocode(String address);
}
```

### 路线规划接口
预留真实路线规划API接口：
```java
// 后续可对接高德地图路线规划API
public interface RoutePlanningService {
    RoutePlan planRoute(Location from, Location to);
}
```

## 注意事项

1. **数据库密码**：配置文件中密码明文存储，生产环境建议使用环境变量或加密
2. **API Key管理**：首版使用模拟数据，对接真实API需妥善管理Key
3. **日志文件**：日志文件位于`logs/`目录，建议定期清理
4. **跨域配置**：已配置允许所有来源跨域访问，生产环境需限制

## 测试

```bash
# 运行单元测试
mvn test
```

## 部署

### JAR包部署
```bash
mvn clean package
java -jar target/system-1.0.0.jar
```

### Docker部署（待完善）
```bash
docker build -t tandian-system .
docker run -p 8080:8080 tandian-system
```

## 版本历史

- v1.0.0 (2026-06-08)
  - 完成核心功能：店铺管理、清单管理、路线规划
  - 使用模拟数据实现路线规划算法
  - 预留地理编码和路线规划API接口

## 许可证

MIT License

## 联系方式

如有问题，请提交Issue或联系开发团队。
