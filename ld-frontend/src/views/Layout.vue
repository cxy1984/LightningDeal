<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="header-left">
        <span class="logo">⚡ LightningDeal</span>
        <span class="divider">|</span>
        <span class="slogan">高并发秒杀系统</span>
      </div>
      <div class="header-right">
        <el-menu :default-active="route.path" router mode="horizontal" class="nav-menu">
          <el-menu-item index="/activity">🔥 秒杀活动</el-menu-item>
          <el-menu-item index="/dashboard">📊 实时大屏</el-menu-item>
          <el-menu-item v-if="userStore.token" index="/orders">📦 我的订单</el-menu-item>
        </el-menu>
        <div class="user-info">
          <template v-if="userStore.token">
            <el-dropdown>
              <span class="user-name">
                <el-avatar :size="32" icon="User" />
                {{ userStore.user?.nickname || userStore.user?.username || '用户' }}
              </span>
              <template #dropdown>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </template>
            </el-dropdown>
          </template>
          <el-button v-else text type="primary" @click="$router.push('/login')">登录</el-button>
        </div>
      </div>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; display: flex; flex-direction: column; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #eee;
  padding: 0 24px;
  height: 64px !important;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.logo { font-size: 20px; font-weight: bold; color: #e74c3c; }
.divider { color: #ddd; }
.slogan { font-size: 13px; color: #999; }
.header-right { display: flex; align-items: center; gap: 16px; }
.nav-menu { border-bottom: none !important; }
.user-info { display: flex; align-items: center; }
.user-name { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.main {
  flex: 1;
  background: #f5f7fa;
  padding: 24px;
  overflow-y: auto;
}
</style>
