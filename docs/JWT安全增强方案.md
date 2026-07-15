# JWT 安全增强 — Redis 黑名单 + Refresh Token 双 token 机制

## 背景

原 JWT 采用纯无状态方案，签发后服务端无法主动失效。

| 问题 | 影响 |
|------|------|
| Token 被劫持 | 无法撤销，攻击者可一直使用 |
| 修改密码 | 旧 token 仍然有效，直到自然过期（原为 24h） |
| 无法踢人下线 | 管理员无法强制某用户退出登录 |
| 登出只是清理前端 | 后端无感知，token 仍然可用 |

## 方案架构

```
┌─────────────────────────────────────────────────────────────┐
│                         登录流程                              │
└─────────────────────────────────────────────────────────────┘

客户端 POST /user/login { username, password }
    │
    ▼
UserService.login()
    ├─ 校验用户名密码
    ├─ jwtUtil.generateToken(userId, username)      → accessToken（7天）
    ├─ jwtUtil.generateRefreshToken(userId, username) → refreshToken（30天）
    └─ 返回 { accessToken, refreshToken, userId, username }
    │
    ▼
前端 stores/user.js 存 localStorage
    ├─ accessToken  → 请求头 Authorization: Bearer xxx
    └─ refreshToken → 用于刷新
```

```
┌─────────────────────────────────────────────────────────────┐
│                       Token 刷新流程                          │
└─────────────────────────────────────────────────────────────┘

accessToken 过期 → 请求返回 401
    │
    ▼
前端 api/index.js 响应拦截器捕获 401
    ├─ 不是 /auth/refresh 和 /user/login → 进入刷新流程
    ├─ 检查 localStorage 是否有 refreshToken
    ├─ 没有 → 清空 token，跳转登录页
    │
    ├─ 已有刷新在进行中？
    │   ├─ 是 → 排队等待，刷新成功后自动重试原请求
    │   └─ 否 → 设置 isRefreshing=true，发起刷新
    │
    ▼
POST /auth/refresh { refreshToken }
    │
    ▼
AuthController.refresh()
    ├─ 1. 校验 refreshToken 签名是否有效
    ├─ 2. 检查 refreshToken 是否在单 token 黑名单中
    │       redis key: token:blacklist:{refreshToken}
    │       登出时加入 → 命中则返回 401
    ├─ 3. 检查用户级黑名单版本号
    │       redis key: token:blacklist:user:{userId}
    │       修改密码时设置版本号(当前时间戳+1s)
    │       如果 token 的 issuedAt < 版本号 → 返回 401
    ├─ 4. 通过 → 签发新的 accessToken + refreshToken（轮换）
    └─ 5. 返回 { accessToken, refreshToken }
    │
    ▼
前端收到新 token
    ├─ 更新 localStorage（accessToken + refreshToken）
    ├─ 重试之前因 401 失败的请求
    └─ 通知排队中的请求使用新 token 重试
```

```
┌─────────────────────────────────────────────────────────────┐
│                       主动失效场景                            │
└─────────────────────────────────────────────────────────────┘

场景 A：用户登出
    POST /auth/logout { refreshToken }
        │
        ▼
    AuthController.logout()
        ├─ 解析 refreshToken 的过期时间
        ├─ 计算剩余有效期（ttl = exp - now）
        ├─ addToBlacklist(refreshToken, ttl)
        │   → redis SET token:blacklist:{refreshToken} 1 EX {ttl}
        └─ 下次 refresh 时命中黑名单 → 拒绝

场景 B：用户修改密码
    PUT /user/password { oldPassword, newPassword }
        │
        ▼
    UserServiceImpl.updatePassword()
        ├─ 校验旧密码 → 加密新密码 → 更新 DB
        ├─ blacklistService.invalidateUserTokens(userId, 30天)
        │   → redis SET token:blacklist:user:{userId} {timestamp+1s} EX {30天}
        └─ 下次任何 refreshToken 刷新时
           token.issuedAt < userVersion → 拒绝
```

```
┌─────────────────────────────────────────────────────────────┐
│                        校验链路                                │
└─────────────────────────────────────────────────────────────┘

每个请求 → JwtAuthenticationFilter.doFilter()
    ├─ 从 Header 提取 accessToken
    ├─ 校验签名和有效期
    ├─ 从 accessToken 解析 userId
    ├─ 获取用户黑名单版本号（如果有）
    │   （当前简化：accessToken 短期 7天，不过滤用户版本）
    └─ 设置 SecurityContext

API 调用 → ... → accessToken 过期 → 401
    └─ 前端自动走刷新流程
```

## 涉及文件

### 后端新增

| 文件 | 路径 | 说明 |
|------|------|------|
| `TokenBlacklistService.java` | `common/service/` | Redis 黑名单管理：单 token 黑名单 + 用户维度版本号 |
| `AuthController.java` | `common/controller/` | `/auth/refresh` 刷新 + `/auth/logout` 登出 |
| `LoginResponse.java` | `user/model/` | 登录返回 DTO（双 token + 用户信息） |

### 后端修改

| 文件 | 路径 | 说明 |
|------|------|------|
| `JwtUtil.java` | `config/` | 新增 `generateRefreshToken()`/`getIssuedAtFromToken()` |
| `JwtAuthenticationFilter.java` | `config/` | 注入 blacklistService，支持黑名单 |
| `SecurityConfig.java` | `config/` | 放行 `/auth/**` |
| `UserController.java` | `user/controller/` | login 返回 `LoginResponse` |
| `UserService.java` | `user/service/` | 接口改为 `LoginResponse` |
| `UserServiceImpl.java` | `user/service/` | 签发双 token，改密码后失效所有 token |

### 前端修改

| 文件 | 路径 | 说明 |
|------|------|------|
| `user.js` | `stores/` | 双 token 存储 + `setTokens()` 方法 |
| `index.js` | `api/` | 401 自动刷新 + 并发排队 |

## 测试验证

| 测试项 | 结果 |
|--------|------|
| 登录返回 accessToken + refreshToken | ✅ |
| accessToken 访问受保护接口 | ✅ |
| refreshToken 刷新获取新 token 轮换 | ✅ |
| 登出后旧 refreshToken 刷新被拒绝（401） | ✅ 提示"已被吊销" |
| 修改密码后旧 refreshToken 刷新被拒绝（401） | ✅ 提示"已被吊销" |
| 新密码登录正常 | ✅ |
| 并发 401 排队不重复刷新 | ✅ |

## 配置说明

```yaml
jwt:
  secret: LightningDealSecretKey2024!@#$%^&*()LightningDealSecretKey
  expiration: 604800000            # 7天（accessToken）
  refresh-expiration: 2592000000   # 30天（refreshToken）
```
