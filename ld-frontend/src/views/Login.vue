<template>
  <div class="login-page">
    <!-- 左侧品牌展示区 -->
    <div class="brand-section">
      <div class="brand-content">
        <h1 class="brand-title">Lightning<span class="highlight">Deal</span></h1>
        <p class="brand-subtitle">高并发秒杀系统</p>
        <div class="brand-divider"></div>
        <p class="brand-desc">
          极速秒杀 · 稳定可靠 · 毫秒级响应
        </p>
        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>毫秒级高并发处理</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>全链路性能监控</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>实时数据可视化大屏</span>
          </div>
        </div>
      </div>
      <!-- 底部装饰模糊圆 -->
      <div class="brand-glow glow-1"></div>
      <div class="brand-glow glow-2"></div>
    </div>

    <!-- 右侧登录注册卡片 -->
    <div class="auth-section">
      <div class="auth-card">
        <!-- 头部 -->
        <div class="auth-header">
          <div class="auth-welcome">
            <h2 class="auth-title">{{ activeTab === 'login' ? '欢迎回来' : '创建账号' }}</h2>
            <p class="auth-desc">{{ activeTab === 'login' ? '登录您的账号，开始秒杀之旅' : '注册新账号，加入闪电交易' }}</p>
          </div>
        </div>

        <!-- Tab 切换 -->
        <div class="auth-tabs">
          <button 
            :class="['tab-btn', { active: activeTab === 'login' }]" 
            @click="activeTab = 'login'"
          >登录</button>
          <button 
            :class="['tab-btn', { active: activeTab === 'register' }]" 
            @click="activeTab = 'register'"
          >注册</button>
          <div class="tab-indicator" :style="{ transform: `translateX(${activeTab === 'login' ? '0' : '100%'})` }"></div>
        </div>

        <!-- 登录表单 -->
        <form v-show="activeTab === 'login'" class="auth-form" @submit.prevent="handleLogin">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <div :class="['input-wrapper', { 'is-focus': loginFocus.username, 'is-filled': loginForm.username }]">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="18" height="18">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
              </svg>
              <input 
                v-model="loginForm.username" 
                type="text" 
                placeholder="请输入用户名"
                @focus="loginFocus.username = true"
                @blur="loginFocus.username = false"
                @input="clearError('username')"
              />
            </div>
            <span v-if="errors.username" class="form-error">{{ errors.username }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">密码</label>
            <div :class="['input-wrapper', { 'is-focus': loginFocus.password, 'is-filled': loginForm.password }]">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="18" height="18">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
              <input 
                v-model="loginForm.password" 
                :type="showLoginPwd ? 'text' : 'password'" 
                placeholder="请输入密码"
                @focus="loginFocus.password = true"
                @blur="loginFocus.password = false"
                @input="clearError('password')"
              />
              <button type="button" class="pwd-toggle" @click="showLoginPwd = !showLoginPwd" tabindex="-1">
                <svg v-if="showLoginPwd" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="16" height="16">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="16" height="16">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                </svg>
              </button>
            </div>
            <span v-if="errors.password" class="form-error">{{ errors.password }}</span>
          </div>

          <!-- 记住我与忘记密码 -->
          <div class="form-options">
            <label class="remember-me">
              <input type="checkbox" v-model="loginForm.remember" />
              <span class="checkmark"></span>
              <span>记住我</span>
            </label>
            <button type="button" class="forgot-pwd">忘记密码？</button>
          </div>

          <button type="submit" class="submit-btn" :class="{ loading }" :disabled="loading">
            <span v-if="!loading">登 录</span>
            <span v-else class="loader"></span>
          </button>
        </form>

        <!-- 注册表单 -->
        <form v-show="activeTab === 'register'" class="auth-form" @submit.prevent="handleRegister">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <div :class="['input-wrapper', { 'is-focus': regFocus.username, 'is-filled': registerForm.username }]">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="18" height="18">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
              </svg>
              <input v-model="registerForm.username" type="text" placeholder="请输入用户名" @focus="regFocus.username = true" @blur="regFocus.username = false" />
            </div>
            <span v-if="regErrors.username" class="form-error">{{ regErrors.username }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">密码</label>
            <div :class="['input-wrapper', { 'is-focus': regFocus.password, 'is-filled': registerForm.password }]">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="18" height="18">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
              <input v-model="registerForm.password" :type="showRegPwd ? 'text' : 'password'" placeholder="密码至少6位" @focus="regFocus.password = true" @blur="regFocus.password = false" />
              <button type="button" class="pwd-toggle" @click="showRegPwd = !showRegPwd" tabindex="-1">
                <svg v-if="showRegPwd" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="16" height="16">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="16" height="16">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                </svg>
              </button>
            </div>
            <span v-if="regErrors.password" class="form-error">{{ regErrors.password }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">手机号</label>
            <div :class="['input-wrapper', { 'is-focus': regFocus.phone, 'is-filled': registerForm.phone }]">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="18" height="18">
                <rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/>
              </svg>
              <input v-model="registerForm.phone" type="tel" placeholder="请输入手机号" @focus="regFocus.phone = true" @blur="regFocus.phone = false" />
            </div>
            <span v-if="regErrors.phone" class="form-error">{{ regErrors.phone }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">昵称</label>
            <div :class="['input-wrapper', { 'is-focus': regFocus.nickname, 'is-filled': registerForm.nickname }]">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="18" height="18">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
              </svg>
              <input v-model="registerForm.nickname" type="text" placeholder="选填" @focus="regFocus.nickname = true" @blur="regFocus.nickname = false" />
            </div>
          </div>

          <button type="submit" class="submit-btn" :class="{ loading }" :disabled="loading">
            <span v-if="!loading">注 册</span>
            <span v-else class="loader"></span>
          </button>
        </form>

        <!-- 底部提示 -->
        <p class="auth-footer">
          {{ activeTab === 'login' ? '还没有账号？' : '已有账号？' }}
          <button class="switch-tab" @click="activeTab = activeTab === 'login' ? 'register' : 'login'">
            {{ activeTab === 'login' ? '立即注册' : '立即登录' }}
          </button>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('login')
const loading = ref(false)
const showLoginPwd = ref(false)
const showRegPwd = ref(false)
// 聚焦状态
const loginFocus = reactive({ username: false, password: false })
const regFocus = reactive({ username: false, password: false, phone: false, nickname: false })

// 表单错误
const errors = reactive({ username: '', password: '' })
const regErrors = reactive({ username: '', password: '', phone: '' })

// 表单数据
const loginForm = reactive({
  username: 'admin',
  password: '123456',
  remember: false
})

const registerForm = reactive({
  username: '',
  password: '',
  phone: '',
  nickname: ''
})

function clearError(field) {
  if (field in errors) errors[field] = ''
}

function validateLogin() {
  let valid = true
  if (!loginForm.username.trim()) {
    errors.username = '请输入用户名'
    valid = false
  }
  if (!loginForm.password) {
    errors.password = '请输入密码'
    valid = false
  }
  return valid
}

function validateRegister() {
  let valid = true
  if (!registerForm.username.trim()) {
    regErrors.username = '请输入用户名'
    valid = false
  }
  if (!registerForm.password || registerForm.password.length < 6) {
    regErrors.password = '密码至少6位'
    valid = false
  }
  if (!/^1[3-9]\d{9}$/.test(registerForm.phone)) {
    regErrors.phone = '手机号格式不正确'
    valid = false
  }
  return valid
}

async function handleLogin() {
  if (!validateLogin()) return
  loading.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/activity')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!validateRegister()) return
  loading.value = true
  try {
    await userStore.register(registerForm)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

// 切换 tab 时清空错误
watch(activeTab, () => {
  Object.keys(errors).forEach(k => errors[k] = '')
  Object.keys(regErrors).forEach(k => regErrors[k] = '')
})
</script>

<style scoped>
/* ===================== CSS 变量 ===================== */
.login-page {
  --color-bg-start: #f0f2f5;
  --color-bg-end: #e8ecf1;
  --color-brand: #e74c3c;
  --color-brand-hover: #c0392b;
  --color-brand-light: rgba(231, 76, 60, 0.08);
  --color-brand-glow: rgba(231, 76, 60, 0.15);
  --color-text: #1a1a2e;
  --color-text-secondary: #6b7280;
  --color-text-muted: #9ca3af;
  --color-border: rgba(0, 0, 0, 0.06);
  --color-card-bg: rgba(255, 255, 255, 0.85);
  --color-input-bg: rgba(0, 0, 0, 0.03);
  --color-input-focus-bg: #fff;
  --color-input-border: rgba(0, 0, 0, 0.08);
  --color-input-focus-border: var(--color-brand);
  --color-shadow: rgba(0, 0, 0, 0.06);
  --color-shadow-lg: rgba(0, 0, 0, 0.1);
  --radius: 12px;
  --radius-sm: 8px;
  --transition: 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ===================== 页面布局 ===================== */
.login-page {
  display: flex;
  height: 100vh;
  background: linear-gradient(135deg, var(--color-bg-start) 0%, var(--color-bg-end) 100%);
  font-family: 'Inter', 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: var(--color-text);
  overflow: hidden;
}

/* ===================== 左侧品牌区 ===================== */
.brand-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 48px;
}

.brand-content {
  position: relative;
  z-index: 2;
  max-width: 400px;
  animation: fadeInUp 0.8s ease-out;
}

.brand-title {
  font-size: 40px;
  font-weight: 700;
  letter-spacing: -1px;
  margin-bottom: 8px;
  color: var(--color-text);
}

.brand-title .highlight {
  color: var(--color-brand);
  position: relative;
}

.brand-subtitle {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin-bottom: 20px;
  font-weight: 400;
}

.brand-divider {
  width: 48px;
  height: 3px;
  background: var(--color-brand);
  border-radius: 2px;
  margin-bottom: 20px;
}

.brand-desc {
  font-size: 14px;
  color: var(--color-text-muted);
  line-height: 1.6;
  margin-bottom: 28px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-brand);
  flex-shrink: 0;
  opacity: 0.7;
}

/* 装饰光晕 */
.brand-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  z-index: 1;
}

.glow-1 {
  width: 400px;
  height: 400px;
  background: var(--color-brand-glow);
  top: -10%;
  right: -10%;
  animation: float 8s ease-in-out infinite;
}

.glow-2 {
  width: 300px;
  height: 300px;
  background: var(--color-brand-glow);
  bottom: -5%;
  left: -5%;
  animation: float 10s ease-in-out infinite reverse;
}

/* ===================== 右侧认证区 ===================== */
.auth-section {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 48px 40px 0;
}

.auth-card {
  width: 100%;
  background: var(--color-card-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--color-border);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 
    0 4px 24px var(--color-shadow),
    0 1px 2px var(--color-shadow);
  animation: fadeInUp 0.8s ease-out 0.15s both;
}

/* 头部 */
.auth-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
}

.auth-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--color-text);
}

.auth-desc {
  font-size: 14px;
  color: var(--color-text-muted);
}

/* ===================== Tab 切换 ===================== */
.auth-tabs {
  display: flex;
  position: relative;
  background: var(--color-input-bg);
  border-radius: var(--radius-sm);
  padding: 4px;
  margin-bottom: 28px;
}

.tab-btn {
  flex: 1;
  padding: 10px 0;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-muted);
  cursor: pointer;
  position: relative;
  z-index: 2;
  transition: color var(--transition);
  border-radius: 6px;
}

.tab-btn.active {
  color: var(--color-text);
}

.tab-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: #fff;
  border-radius: 6px;
  z-index: 1;
  transition: transform var(--transition);
  box-shadow: 0 1px 3px var(--color-shadow);
}

/* ===================== 表单 ===================== */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  padding-left: 2px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  height: 48px;
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--color-input-border);
  background: var(--color-input-bg);
  transition: all var(--transition);
}

.input-wrapper:hover {
  border-color: var(--color-text-muted);
}

.input-wrapper.is-focus,
.input-wrapper:focus-within {
  border-color: var(--color-brand);
  background: var(--color-input-focus-bg);
  box-shadow: 0 0 0 3px var(--color-brand-light);
}

.input-wrapper.is-filled {
  border-color: var(--color-input-border);
}

.input-icon {
  color: var(--color-text-muted);
  flex-shrink: 0;
  transition: color var(--transition);
}

.input-wrapper.is-focus .input-icon,
.input-wrapper:focus-within .input-icon {
  color: var(--color-brand);
}

.input-wrapper input {
  flex: 1;
  height: 100%;
  border: none;
  background: transparent;
  font-size: 14px;
  color: var(--color-text);
  outline: none;
}

.input-wrapper input::placeholder {
  color: var(--color-text-muted);
  font-size: 14px;
}

.pwd-toggle {
  background: none;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  transition: color var(--transition);
  flex-shrink: 0;
}

.pwd-toggle:hover {
  color: var(--color-text-secondary);
}

.form-error {
  font-size: 12px;
  color: var(--color-brand);
  padding-left: 2px;
  animation: shake 0.3s ease-out;
}

/* ===================== 表单选项 ===================== */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: -4px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-muted);
  cursor: pointer;
  user-select: none;
}

.remember-me input {
  display: none;
}

.checkmark {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1.5px solid var(--color-input-border);
  position: relative;
  transition: all var(--transition);
  flex-shrink: 0;
}

.remember-me input:checked + .checkmark {
  background: var(--color-brand);
  border-color: var(--color-brand);
}

.remember-me input:checked + .checkmark::after {
  content: '';
  position: absolute;
  left: 4.5px;
  top: 1.5px;
  width: 5px;
  height: 9px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.forgot-pwd {
  background: none;
  border: none;
  font-size: 13px;
  color: var(--color-brand);
  cursor: pointer;
  transition: opacity var(--transition);
}

.forgot-pwd:hover {
  opacity: 0.8;
}

/* ===================== 提交按钮 ===================== */
.submit-btn {
  height: 48px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--color-brand);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  margin-top: 4px;
}

.submit-btn:hover:not(:disabled) {
  background: var(--color-brand-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px var(--color-brand-glow);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* Loading 动画 */
.loader {
  width: 20px;
  height: 20px;
  border: 2.5px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/* ===================== 底部 ===================== */
.auth-footer {
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border);
}

.switch-tab {
  background: none;
  border: none;
  color: var(--color-brand);
  font-weight: 500;
  cursor: pointer;
  font-size: 13px;
  transition: opacity var(--transition);
}

.switch-tab:hover {
  opacity: 0.8;
}

/* ===================== 动画 ===================== */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse-glow {
  0%, 100% {
    filter: drop-shadow(0 0 20px var(--color-brand-glow));
  }
  50% {
    filter: drop-shadow(0 0 30px var(--color-brand-glow));
  }
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -30px) scale(1.05);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.95);
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

/* ===================== 响应式 ===================== */
@media (max-width: 960px) {
  .brand-section {
    display: none;
  }
  .auth-section {
    width: 100%;
    padding: 24px;
  }
  .auth-card {
    padding: 32px 24px;
  }
}

@media (max-width: 480px) {
  .auth-section {
    padding: 16px;
  }
  .auth-card {
    padding: 24px 20px;
    border-radius: 16px;
  }
}
</style>
