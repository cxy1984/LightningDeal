# 两个问题详解

---

## 问题一：前端如何判断登录成功并跳转？

### 源码链路

```javascript
// Login.vue - 点击登录按钮
async function handleLogin() {
  await userStore.login(loginForm)  // ① 调用 store 的 login
  ElMessage.success('登录成功')       // ④ 显示成功提示
  router.push('/activity')           // ⑤ 跳转到活动列表
}

// stores/user.js
async function login(loginData) {
  const res = await api.login(loginData)  // ② 发 POST 请求
  token.value = res.data                   // ③ 保存 Token
  localStorage.setItem('token', res.data)
  await fetchUserInfo()
  return res
}
```

### 逐步骤详解

```
① handleLogin() 调用 userStore.login(loginForm)
  ↓
② POST /api/user/login → 后端返回：
   {
     "code": 200,
     "msg": "登录成功",
     "data": "eyJhbGciOiJIUzM4NCJ9..."   ← Token
   }
  ↓
③ userStore.login() 接收响应 res
   token.value = res.data          → 存到 Pinia（内存）
   localStorage.setItem('token', res.data)  → 存到浏览器本地存储
   fetchUserInfo()                 → 顺便查用户信息
  ↓
④ 回到 Login.vue 的 handleLogin()
   await userStore.login() 执行完毕 ← 没抛异常 = 登录成功
   ElMessage.success('登录成功')
  ↓
⑤ router.push('/activity')  → 跳转到活动列表页
```

### 关键判断依据

| 什么代表"登录成功" | 说明 |
|-------------------|------|
| `userStore.login()` 没有抛异常 | 后端返回 `code=200`，axios 响应拦截器没拦截 |
| `res.data` 有值 | 后端返回的 Token 字符串 |

### 如果登录失败

```javascript
// 后端返回：
{ "code": 401, "msg": "用户名或密码错误" }

// api/index.js 响应拦截器：
response => {
    if (res.code !== 200) {
      ElMessage.error('用户名或密码错误')  // 显示错误提示
      return Promise.reject(...)          // 抛异常
    }
}

// Login.vue 的 catch 捕获：
catch (e) {
    // 什么都不做，ElMessage 已经显示了错误
    // handleLogin 不会继续执行 → 不会跳转
}
```

所以前端判断登录成功的方式是：**调用 `userStore.login()` 没抛异常 = 跳转**。

---

## 问题二：FilterSecurityInterceptor 内部怎么检查 Authentication？

### 问题本质

> 路径权限配置了 `.anyRequest().authenticated()`，Security 是怎么知道要去 `SecurityContextHolder` 里拿 `Authentication` 来检查的？

### 源码机制（简化）

`FilterSecurityInterceptor` 本身不干活，它委托给父类 `AbstractSecurityInterceptor`，核心逻辑：

```java
// FilterSecurityInterceptor（简化版）
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    // 1. 从 SecurityContextHolder 获取当前认证信息
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // 2. 把 authentication + 请求信息 交给 AccessDecisionManager 裁决
    try {
        accessDecisionManager.decide(authentication, object, configAttributes);
        //    ↑ 如果裁决通过，正常返回，继续往下走
        //    ↑ 如果裁决不通过，抛出 AccessDeniedException
    } catch (AccessDeniedException e) {
        // 3. 裁决不通过的处理
        if (authentication == null || !authentication.isAuthenticated()) {
            // 没登录 → 401
            securityContextHolderStrategy.clearContext();
            exceptionTranslationFilter.sendStartAuthentication();
        } else {
            // 已登录但权限不够 → 403
            exceptionTranslationFilter.sendAccessDenied();
        }
        return;
    }

    // 4. 裁决通过 → 放行到下一个过滤器
    chain.doFilter(request, response);
}
```

### AccessDecisionManager 怎么裁决？

Spring Security 默认的裁决器是 `AffirmativeBased`（一票通过制），内部有一个**投票器列表**：

```java
// AffirmativeBased.decide()
for (AccessDecisionVoter voter : voters) {
    int result = voter.vote(authentication, object, configAttributes);
    if (result == ACCESS_GRANTED) {
        return;  // 有一个投票通过 → 整体通过
    }
}
```

### WebExpressionVoter（本项目的投票器）

Spring Security 的路径权限表达式（如 `.authenticated()`、`.permitAll()`）是由 `WebExpressionVoter` 处理的：

```java
// WebExpressionVoter.vote() — 简化版
public int vote(Authentication authentication, FilterInvocation fi, Collection<ConfigAttribute> attributes) {
    // 1. 解析配置的表达式，比如 "authenticated" 或 "permitAll"
    Expression expression = getExpression(attributes);

    // 2. 在 SecurityExpressionRoot 上下文中求值
    SecurityExpressionRoot root = new SecurityExpressionRoot(authentication) {
        // 这里 authentication 就是 SecurityContextHolder.getContext().getAuthentication()
    };

    // 3. 对 "authenticated" 求值
    //    root.authenticated → 内部调用 authentication != null && authentication.isAuthenticated()
    // 对 "permitAll" 求值
    //    root.permitAll → 恒返回 true

    boolean result = expression.getValue(root, Boolean.class);
    return result ? ACCESS_GRANTED : ACCESS_DENIED;
}
```

### "authenticated" 的求值逻辑

配置 `.anyRequest().authenticated()` 会被解析为表达式 `"authenticated"`，它的求值逻辑：

```java
// SecurityExpressionRoot.authenticated
public final boolean authenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    // ↑ 注意：这里又取了一次 SecurityContextHolder

    if (authentication == null) {
        return false;    // 根本没登录 → 拒绝
    }

    if (authentication instanceof AnonymousAuthenticationToken) {
        return false;    // 是匿名用户（没带 Token 的情况）→ 拒绝
    }

    return authentication.isAuthenticated();  // 正常 JWT 用户 → true → 通过
}
```

### 完整裁决流程

```
请求到达 FilterSecurityInterceptor
  ↓
① AbstractSecurityInterceptor 从 SecurityContextHolder.getContext().getAuthentication() 取 Authentication
                                                    ↑
                                              (JwtAuthFilter 之前设进去的)
  ↓
② 把 Authentication 传给 AccessDecisionManager.decide()
  ↓
③ AffirmativeBased 遍历投票器列表
  ↓
④ WebExpressionVoter.vote(authentication, ...)
  ↓
⑤ 对 "authenticated" 表达式求值
   root.authenticated()
   ↓
   authentication = SecurityContextHolder.getContext().getAuthentication()
   ↓
   如果 authentication == null → 返回 false → ACCESS_DENIED → 401
   如果 authentication 是匿名用户 → 返回 false → ACCESS_DENIED → 401
   如果 authentication 是正常用户 → 返回 true → ACCESS_GRANTED → ✅ 放行
```

### "permitAll" 的求值逻辑

```java
// SecurityExpressionRoot.permitAll
public final boolean permitAll() {
    return true;  // 永远通过，不看 Authentication
}
```

这就是为什么公开接口**不需要 Token 也能访问**——`permitAll` 恒返回 `true`，根本不检查 `Authentication`。

### 一句话总结

> `FilterSecurityInterceptor` 在内部通过 `SecurityContextHolder.getContext().getAuthentication()` 获取认证信息，然后交给 `WebExpressionVoter` 对表达式（如 `authenticated()`）求值。求值时会检查 Authentication 是否为 null、是否为匿名用户、是否已认证。`permitAll()` 则直接返回 `true`，跳过所有检查。
