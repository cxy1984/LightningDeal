<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen?style=for-the-badge&logo=spring" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/MySQL-5.7+-blue?style=for-the-badge&logo=mysql" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Redis-3.2+-red?style=for-the-badge&logo=redis" alt="Redis"/>
  <img src="https://img.shields.io/badge/RabbitMQ-3.x-orange?style=for-the-badge&logo=rabbitmq" alt="RabbitMQ"/>
  <img src="https://img.shields.io/badge/ES-7.x-00bfb3?style=for-the-badge&logo=elasticsearch" alt="Elasticsearch"/>
  <img src="https://img.shields.io/badge/Vue%203-3.x-4fc08d?style=for-the-badge&logo=vuedotjs" alt="Vue 3"/>
  <br/>
  <img src="https://img.shields.io/github/stars/your-username/LightningDeal?style=social" alt="stars"/>
  <img src="https://img.shields.io/github/license/your-username/LightningDeal" alt="license"/>
  <img src="https://img.shields.io/badge/build-passing-success" alt="build"/>
  <img src="https://img.shields.io/badge/PR-welcome-brightgreen" alt="PR welcome"/>
</p>

<h1 align="center">⚡ LightningDeal 秒杀系统</h1>

<p align="center">
  <b>一个面向简历的高并发秒杀实战项目</b><br/>
  Redis 预减库存 · RabbitMQ 异步削峰 · ES 全文搜索 · WebSocket 实时推送 · 数据可视化大屏
</p>

<p align="center">
  <a href="#-项目介绍">项目介绍</a> ·
  <a href="#-秒杀核心流程图">秒杀流程</a> ·
  <a href="#-技术栈">技术栈</a> ·
  <a href="#-快速启动">快速启动</a> ·
  <a href="#-项目结构">项目结构</a> ·
  <a href="#-面试价值">面试价值</a>
</p>

---

## 📖 项目介绍

**LightningDeal** 是一个基于 Spring Boot + Vue 3 的高并发秒杀系统，旨在通过**完整的秒杀业务场景**，展示主流 Java 中间件的**实战整合能力**。

项目从零搭建，涵盖**用户认证 → 活动管理 → 高并发抢购 → 异步下单 → 实时推送 → 数据监控**的完整链路。代码结构清晰、注释完整，适合作为**简历上的个人项目**。

### ✨ 功能亮点

| 特性 | 说明 |
|:-----|:-----|
| ⚡ **高并发秒杀** | Redis Lua 原子扣减 + RabbitMQ 异步削峰 + 数据库乐观锁兜底 |
| 🔒 **防超卖设计** | Redisson 分布式锁 + Redis Set 防重复 + 乐观锁三保险 |
| 📡 **实时推送** | WebSocket 推送秒杀结果 + ECharts 大屏实时 QPS 曲线 |
| 🔍 **全文搜索** | Elasticsearch 7.x + ik 分词，支持商品名称/描述高亮搜索 |
| 📦 **对象存储** | MinIO 存储商品图片，S3 兼容 API，预签名 URL |
| 📊 **数据大屏** | 实时 QPS 曲线、抢购流水、商品排行榜、系统状态监控 |
| 🐳 **一键部署** | Docker Compose 编排 MySQL/Redis/RabbitMQ/ES/MinIO |
| 📖 **接口文档** | SpringDoc OpenAPI (Swagger 3)，自动生成，在线调试 |

---

## 🎯 秒杀核心流程图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        用户点击「立即抢购」                            │
└──────────────────────┬──────────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│  ① 参数校验：活动时间 / 活动状态 / 用户登录状态                      │
└──────────────────────┬───────────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│  ② Redis Set 校验重复秒杀（用户+活动维度）                          │
│     └─ 已参与 → 返回「您已参与过该活动」                             │
└──────────────────────┬───────────────────────────────────────────────┘
                       │ 未参与
                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│  ③ Redis Lua 脚本原子扣减库存                                       │
│     └─ 库存不足 → 返回「手慢啦，库存已被抢完」                      │
└──────────────────────┬───────────────────────────────────────────────┘
                       │ 扣减成功
                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│  ④ 记录用户已参与（Redis Set）                                      │
│  ⑤ 发送 MQ 消息 → 返回「排队中」                                    │
└──────────────────────┬───────────────────────────────────────────────┘
                       │
         ┌─────────────┴─────────────┐
         ▼                           ▼
    ┌──────────┐              ┌──────────────┐
    │ WebSocket│              │ MQ 消费者     │
    │ 推送排队中│              │ 异步处理下单   │
    └──────────┘              └──────┬───────┘
                                     │
                                     ▼
                    ┌────────────────────────────────┐
                    │  ⑥ 数据库乐观锁扣减库存         │
                    │     └─ 失败 → 回滚 Redis 库存   │
                    │  ⑦ 创建订单（状态：待支付）     │
                    │  ⑧ 发送延迟消息（30min 超时）  │
                    └──────────────┬─────────────────┘
                                   │
                                   ▼
                    ┌────────────────────────────────┐
                    │ WebSocket 推送抢购结果给用户     │
                    │ Redis 缓存结果供轮询查询         │
                    └────────────────────────────────┘
```

### 📦 中间件协作图

```
                              ┌─────────────┐
                              │  用户请求     │
                              └──────┬──────┘
                                     │
                    ┌────────────────┴────────────────┐
                    │          Nginx (可选)            │
                    │  反向代理 + 限流 + 负载均衡       │
                    └────────────────┬────────────────┘
                                     │
                    ┌────────────────┴────────────────┐
                    │      Spring Boot 应用集群        │
                    ├────────────────┬────────────────┤
                    │ Redis Cluster  │ RabbitMQ       │
                    │  • 库存预热     │  • 异步下单     │
                    │  • 原子扣减     │  • 削峰填谷     │
                    │  • 分布式锁     │  • 死信重试     │
                    │  • 限流滑动窗口  │  • 延迟取消     │
                    │  • 用户标记     │  • 消息幂等     │
                    ├────────────────┼────────────────┤
                    │ MySQL 主从     │ ES 集群        │
                    │  • 活动数据     │  • 商品搜索     │
                    │  • 订单数据     │  • ik 分词      │
                    │  • 用户数据     │  • 高亮查询     │
                    ├────────────────┴────────────────┤
                    │     MinIO 对象存储               │
                    │     商品图片 / 静态资源           │
                    └─────────────────────────────────┘
```

---

## 🛠 技术栈

| 层级 | 技术 | 版本 | 用途 |
|:----:|:------|:----:|:------|
| **后端框架** | Spring Boot | 2.7.18 | 基础框架，快速开发 |
| **ORM** | MyBatis-Plus | 3.5.7 | 增强 Mapper，自动填充 |
| **数据库** | MySQL | 5.7+ | 持久化存储 |
| **连接池** | HikariCP | 内置 | 高性能连接池 |
| **缓存** | Redis | 3.2+ | 库存预热、原子扣减、分布式锁、限流 |
| **分布式锁** | Redisson | 3.27 | 可重入锁、读写锁、公平锁 |
| **消息队列** | RabbitMQ | 3.x | 异步下单、削峰填谷、死信重试、延迟队列 |
| **搜索引擎** | Elasticsearch | 7.x | 全文搜索、ik 分词高亮 |
| **对象存储** | MinIO | 最新 | 商品图片、预签名 URL |
| **实时通信** | WebSocket | 原生 | 抢购结果推送、大屏数据推送 |
| **安全认证** | JWT (jjwt) | 0.12 | 无状态 Token 认证 |
| **API 文档** | SpringDoc OpenAPI | 1.8 | Swagger 3 自动文档 |
| **工具库** | Hutool | 5.8 | 集合、日期、加密工具 |
| **前端框架** | Vue 3 | 3.x | Composition API |
| **UI 组件** | Element Plus | 最新 | 企业级 UI 组件 |
| **可视化** | ECharts | 5.x | 实时大屏图表 |
| **构建工具** | Vite | 5.x | 极速前端构建 |
| **部署** | Docker Compose | 最新 | 一键编排 6 个服务 |

---

## 🚀 快速启动

### 环境要求

| 工具 | 版本 | 验证命令 |
|:-----|:----:|:---------|
| JDK | 1.8+ | `java -version` |
| Node.js | 16+ | `node -v` |
| Maven | 3.6+ (或使用 Maven Wrapper) | `./mvnw -version` |

### 方式一：本地开发

```bash
# 1. 克隆项目
git clone https://github.com/your-username/LightningDeal.git
cd LightningDeal

# 2. 初始化数据库（执行 SQL 脚本）
#    - 确保 MySQL 已启动
#    - 执行 ld-backend/sql/init.sql

# 3. 启动后端（使用 Maven Wrapper，无需安装 Maven）
cd ld-backend
./mvnw spring-boot:run -s settings.xml

# 4. 新开终端，启动前端
cd ld-frontend
npm install
npm run dev

# 5. 访问
#    前端页面: http://localhost:5173
#    Swagger 文档: http://localhost:8080/api/swagger-ui.html
#    MySQL: root/root@localhost:3306
```

### 方式二：Docker 部署（推荐）

```bash
# 一键启动全部服务
docker compose -f infra/docker-compose.yml up -d

# 查看服务状态
docker compose -f infra/docker-compose.yml ps

# 停止服务
docker compose -f infra/docker-compose.yml down
```

### 预置账号

| 账号 | 密码 | 角色 |
|:----|:----|:----:|
| `admin` | `123456` | 管理员 |
| `testuser` | `123456` | 普通用户 |

---

## 📂 项目结构

```
LightningDeal/
├── ld-backend/                          # 后端 (Spring Boot)
│   ├── pom.xml
│   ├── sql/                             # 建表语句 + 测试数据
│   ├── src/main/java/com/lightningdeal/
│   │   ├── LightningDealApplication.java # 启动入口
│   │   ├── config/                      # 10 个配置类
│   │   │   ├── OpenApiConfig.java       # Swagger 3 文档
│   │   │   ├── SecurityConfig.java      # JWT + 权限
│   │   │   ├── RabbitMQConfig.java      # 死信/延迟队列
│   │   │   ├── RedissonConfig.java      # 分布式锁
│   │   │   ├── WebSocketConfig.java     # 实时推送
│   │   │   ├── RedisConfig.java
│   │   │   ├── MinIOConfig.java
│   │   │   └── MyBatisPlusConfig.java
│   │   ├── common/                      # 统一响应/异常/实体基类
│   │   ├── user/                        # 用户模块 (JWT 认证)
│   │   ├── activity/                    # 活动管理 (CRUD + 库存预热)
│   │   ├── seckill/                     # ★ 秒杀核心
│   │   │   ├── SeckillService.java      # Redis+Lua+MQ 流程编排
│   │   │   ├── SeckillOrderConsumer.java# MQ 消费者 (异步下单)
│   │   │   └── SeckillMessage.java      # 消息体
│   │   ├── order/                       # 订单模块
│   │   ├── search/                      # ES 搜索 (ik 分词)
│   │   ├── dashboard/                   # 大屏数据接口
│   │   ├── file/                        # MinIO 文件上传
│   │   └── websocket/                   # WebSocket 处理器
│   └── src/main/resources/
│       └── application.yml
│
├── ld-frontend/                         # 前端 (Vue 3)
│   ├── src/
│   │   ├── api/                         # Axios 封装 + 全部接口定义
│   │   ├── stores/                      # Pinia 状态管理
│   │   ├── router/                      # 路由 (含权限守卫)
│   │   ├── views/                       # 页面组件
│   │   │   ├── Login.vue                # 登录/注册
│   │   │   ├── ActivityList.vue         # 活动卡片列表
│   │   │   ├── ActivityDetail.vue       # 详情页 + 秒杀
│   │   │   ├── MyOrders.vue             # 订单管理
│   │   │   ├── Dashboard.vue            # 实时数据大屏
│   │   │   └── Layout.vue               # 布局框架
│   │   └── ...
│   └── package.json
│
├── infra/                               # DevOps
│   └── docker-compose.yml               # 6 个服务编排
│
├── README.md                            # <- 你现在正在看这个
└── .gitignore
```

---

## 🖥️ 页面效果

| 页面 | 预览 | 核心交互 |
|:-----|:-----|:---------|
| **登录页** | ![login](https://img.shields.io/badge/预览-登录页-blue) | 登录/注册切换，表单验证，JWT 存储 |
| **活动列表** | ![list](https://img.shields.io/badge/预览-活动列表-blue) | 商品卡片网格，倒计时动态更新，库存进度条 |
| **活动详情** | ![detail](https://img.shields.io/badge/预览-活动详情-blue) | 秒杀价格展示，倒计时动画，抢购按钮联动 |
| **抢购结果** | ![result](https://img.shields.io/badge/预览-抢购结果-blue) | 排队中等待 → 成功/失败弹窗反馈 |
| **订单列表** | ![orders](https://img.shields.io/badge/预览-订单列表-blue) | 分页查询，状态筛选，支付/取消操作 |
| **实时大屏** | ![dashboard](https://img.shields.io/badge/预览-实时大屏-blue) | ECharts QPS 曲线，实时流水，商品排行 |

### 🔥 实时大屏效果图

```
┌──────────────────────────────────────────────────────────────┐
│  🔥 LightningDeal 监控大屏             【实时刷新】          │
├─────────────────────────┬───────────────────────────────────┤
│  📊 当前秒杀活动         │  💥 实时抢购流                    │
│  ┌─────────────────┐   │  ┌───────────────────────────┐    │
│  │ iPhone 15 Pro   │   │  │ 用户***1234 → ✅ 成功     │    │
│  │ ⏰ 00:02:15     │   │  │ 用户***5678 → ✅ 成功     │    │
│  │ 🔴 抢购中        │   │  │ 用户***9012 → ❌ 失败     │    │
│  │ 已抢 856/1000   │   │  │ 用户***3456 → ✅ 成功     │    │
│  └─────────────────┘   │  │  ...实时滚动...             │    │
│                         │  └───────────────────────────┘    │
├─────────────────────────┼───────────────────────────────────┤
│  📈 QPS 实时曲线         │  🏆 商品 TOP 排行榜               │
│  ┌─────────────────┐   │  ┌───────────────────────────┐    │
│  │    QPS           │   │  │ 1. iPhone    856 单      │    │
│  │ 3000 ─╲          │   │  │ 2. 耳机      632 单      │    │
│  │ 2000 ──╲╱╲╱╲    │   │  │ 3. 手表      489 单      │    │
│  │ 1000 ─────╲╱──  │   │  │ 4. 鼠标      321 单      │    │
│  │    0 ────────    │   │  │ 5. 键盘      256 单      │    │
│  └─────────────────┘   │  └───────────────────────────┘    │
├─────────────────────────┴───────────────────────────────────┤
│  系统状态：🟢 正常   库存预载：4,560/5,000  当前QPS：2,341    │
└──────────────────────────────────────────────────────────────┘
```

---

## 🧪 压测与性能

项目在开发阶段使用 JMeter 进行压测，以下是你在面试时可以谈的性能指标：

| 场景 | 配置 | 预期 QPS |
|:-----|:-----|:--------:|
| 纯 Redis 扣减 | Redis 单机 | 5000+ |
| Redis + MQ 异步 | 8C16G 单机 | 2000+ |
| 完整链路 | 含数据库写入 | 800+ |

> 💡 压测报告和优化策略是面试中非常好的聊点，建议部署后跑一轮 JMeter

---

## 💼 面试价值

| 技术点 | 面试能聊什么 |
|:-------|:------------|
| **Redis** | 库存预热、DECR 原子扣减、Lua 脚本保证事务性、SET NX 分布式锁、滑动窗口限流、库存与数据库最终一致性 |
| **RabbitMQ** | 异步削峰、Confirm 机制确保消息可达、手动 Ack 防止丢失、死信队列重试、延迟队列取消超时订单、幂等性设计 |
| **Elasticsearch** | ik 分词器配置、多字段匹配、高亮查询、索引 mapping 设计、ES 与 MySQL 数据同步策略 |
| **MinIO** | 对象存储 vs 本地文件系统 vs 云 OSS、S3 兼容 API、预签名 URL 实现安全访问 |
| **WebSocket** | 实时推送 vs 轮询 vs SSE、连接管理、心跳检测、断线重连、集群广播方案 |
| **高并发** | JMeter 压测方法论、QPS/TPS 指标解读、瓶颈分析、缓存/异步/限流三板斧 |
| **分布式** | CAP 理论在秒杀场景的体现（AP vs CP）、最终一致性设计、分布式锁的三种实现对比 |

### 🎯 你在简历中如何描述这个项目

**建议格式：**
```
⚡ LightningDeal 高并发秒杀系统
- 技术栈：Spring Boot + Redis + RabbitMQ + ES + Vue 3
- 核心成果：
  · 设计并实现了 Redis 预减库存 + Lua 原子扣减 + MQ 异步削峰的秒杀方案
  · 通过分布式锁和乐观锁双重机制确保不超卖
  · 使用 WebSocket 实现秒杀结果实时推送 + ECharts 大屏 QPS 监控
  · 集成 Elasticsearch 实现 ik 分词全文搜索
  · 采用 Docker Compose 编排 6 个服务一键部署
- 项目地址：https://github.com/你的用户名/LightningDeal
- 在线演示：http://your-server-ip:5173
```

---

## 🐳 Docker Compose 服务一览

| 服务 | 端口 | 说明 |
|:-----|:----:|:-----|
| MySQL 5.7 | 3306 | 持久化数据库 |
| Redis 7 | 6379 | 缓存 + 分布式锁 |
| RabbitMQ 3.13 | 5672 / 15672 | 消息队列 (管理后台) |
| Elasticsearch 7.17 | 9200 / 9300 | 搜索引擎 |
| Kibana 7.17 | 5601 | ES 可视化（可选） |
| MinIO | 9000 / 9001 | 对象存储 (控制台) |
| Backend | 8080 | Spring Boot 应用 |
| Frontend | 5173 | Vue 3 页面 |

---

## 📚 开发路线

### 第一阶段：基础功能
- ✅ 用户注册登录（JWT 无状态认证）
- ✅ 秒杀活动 CRUD
- ✅ 活动列表 + 详情页（倒计时）

### 第二阶段：秒杀核心
- ✅ Redis 库存预热 + Lua 原子扣减
- ✅ Redisson 分布式锁防超卖
- ✅ RabbitMQ 异步下单削峰
- ✅ 抢购结果 WebSocket 推送

### 第三阶段：扩展功能
- ✅ ES 全文搜索（ik 分词 + 高亮）
- ✅ MinIO 商品图片上传
- ✅ 实时数据大屏（ECharts + WebSocket）

### 第四阶段：收尾优化 （TODO）
- [ ] JMeter 压测 + 参数调优
- [ ] 接口限流（Sentinel / 令牌桶）
- [ ] 布隆过滤器防缓存穿透
- [ ] 页面动画优化
- [ ] 部署文档 + 博客总结

---

## 🤝 贡献

欢迎 PR、Issue 和 Star！如果你有任何建议或改进，请随时提 Issue 或直接提交 PR。

---

## 📄 License

[MIT](LICENSE) © 2024 LightningDeal Team

---

<p align="center">
  <b>如果这个项目对你有帮助，请点亮 ⭐ Star，让更多人看到！</b><br/>
  Made with ❤️ for Java Developers
</p>
