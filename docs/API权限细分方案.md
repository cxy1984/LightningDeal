# API 权限细分方案总结

## 方案选择

小型 RBAC，用户表加一个 `role` 字段，用 `@PreAuthorize` 注解控制接口权限。不建独立角色/权限表，未来有需求再扩展。

## 实现流程

```
DB 加字段
  │  ALTER TABLE user ADD COLUMN role VARCHAR(16) DEFAULT 'user'
  │  UPDATE user SET role='admin' WHERE username='admin'
  ▼
User 实体 + UserVO + LoginResponse 加 role 字段
  ▼
JwtUtil 签发/解析 token 携带 role
  │  generateToken(userId, username, role)
  │  getRoleFromToken(token) → "admin" / "user"
  ▼
JwtAuthenticationFilter 设置角色
  │  SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
  ▼
SecurityConfig 开启注解
  │  @EnableGlobalMethodSecurity(prePostEnabled = true)
  ▼
管理接口加 @PreAuthorize
  │  @PreAuthorize("hasRole('ADMIN')")
  │  create / update / delete / status / preheat / sync
  ▼
GlobalExceptionHandler 处理 AccessDeniedException → 403
  ▼
前端 user.js 存储 role
  ▼
Layout.vue 判断 role 显示管理入口
  │  v-if="userStore.role === 'admin'"
```

## 涉及文件（12 个）

| 后端（8个） | 前端（2个） | DB（1个） |
|------------|------------|----------|
| User.java | user.js | user.role |
| UserVO.java | Layout.vue | |
| LoginResponse.java | | |
| JwtUtil.java | | |
| JwtAuthenticationFilter.java | | |
| SecurityConfig.java | | |
| SeckillActivityController.java | | |
| SearchController.java | | |
| GlobalExceptionHandler.java | | |
| UserServiceImpl.java | | |

## 权限矩阵

| 接口 | 公开 | 登录即可 | 仅管理员 |
|------|:---:|:--------:|:--------:|
| 登录/注册 | ✅ | | |
| 活动列表/详情/搜索/大屏 | ✅ | | |
| 秒杀/订单/用户信息 | | ✅ | |
| 创建/编辑/删除/预热/同步 | | | ✅ |

## 测试结果

| 测试项 | 结果 |
|--------|------|
| 管理员创建活动 | ✅ 200 |
| 普通用户创建活动 | ✅ 403"权限不足" |
| 普通用户查看活动列表 | ✅ 200 放行 |
| 管理员预热/ES同步 | ✅ 200 |
| 前端管理入口仅管理员可见 | ✅ |
