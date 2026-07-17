<template>
  <div class="activity-list">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索商品名称..." clearable prefix-icon="Search" style="width: 300px" @keyup.enter="handleSearch" />
      <el-button type="danger" @click="handleSearch">搜索</el-button>
    </div>

    <!-- 骨架屏：加载中 -->
    <el-row :gutter="20" v-if="loading">
      <el-col v-for="n in 8" :key="n" :xs="24" :sm="12" :md="8" :lg="6" class="card-col">
        <el-card :body-style="{ padding: '0' }" shadow="hover">
          <el-skeleton style="width: 100%" animated>
            <template #template>
              <el-skeleton-item variant="image" style="width:100%;height:200px" />
              <div style="padding: 16px">
                <el-skeleton-item variant="text" style="width:70%;margin-bottom:8px" />
                <el-skeleton-item variant="text" style="width:40%;margin-bottom:12px" />
                <el-skeleton-item variant="text" style="width:100%;height:8px;margin-bottom:8px" />
                <el-skeleton-item variant="text" style="width:60%;height:14px" />
              </div>
            </template>
          </el-skeleton>
        </el-card>
      </el-col>
    </el-row>

    <!-- 活动卡片 -->
    <el-row :gutter="20" v-else>
      <el-col v-for="item in list" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6" class="card-col">
        <el-card class="activity-card" :body-style="{ padding: '0' }" shadow="hover" @click="$router.push(`/activity/detail/${item.id}`)">
          <div class="card-image">
            <el-image :src="item.goodsImage || '/placeholder.png'" fit="cover" style="width:100%;height:200px">
              <template #error>
                <div class="image-placeholder">📷 {{ item.goodsName }}</div>
              </template>
            </el-image>
            <div class="card-status" :class="'status-' + item.status">
              {{ statusText(item.status) }}
            </div>
          </div>
          <div class="card-body">
            <h3 class="card-title" v-html="item.name"></h3>
            <div class="card-price">
              <span class="flash-price">¥{{ item.flashPrice }}</span>
              <span class="original-price">¥{{ item.originalPrice }}</span>
            </div>
            <div class="card-progress">
              <el-progress :percentage="progressPercent(item)" :stroke-width="8" :color="progressColor" />
            </div>
            <div class="card-stats">
              <span>已抢 {{ item.soldStock }}/{{ item.totalStock }}</span>
              <span v-if="item.countingDown" class="countdown">
                ⏰ {{ formatCountdown(item.countdownMillis) }}
              </span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 空状态 -->
    <el-empty v-if="!list.length" description="暂无秒杀活动" />

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchList"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '@/api'

const list = ref([])
const page = ref(1)
const size = ref(12)
const total = ref(0)
const keyword = ref('')
const loading = ref(false)

onMounted(() => fetchList())

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) {
      const res = await api.searchActivity({ keyword: keyword.value, page: 0, size: size.value })
      list.value = res.data.content || []
      total.value = res.data.totalElements || 0
    } else {
      const res = await api.getActivityList(params)
      list.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.value = 1; fetchList() }

function statusText(status) {
  return ['草稿', '即将开始', '抢购中', '已结束'][status] || '未知'
}

function progressPercent(item) {
  return item.totalStock > 0 ? Math.round((item.soldStock / item.totalStock) * 100) : 0
}

const progressColor = '#e74c3c'

function formatCountdown(ms) {
  if (ms <= 0) return ''
  const totalSeconds = Math.floor(ms / 1000)
  const h = Math.floor(totalSeconds / 3600)
  const m = Math.floor((totalSeconds % 3600) / 60)
  const s = totalSeconds % 60
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}
</script>

<style scoped>
.activity-list { max-width: 1400px; margin: 0 auto; }
.search-bar { display: flex; gap: 12px; margin-bottom: 24px; align-items: center; }
.card-col { margin-bottom: 20px; }
.activity-card { cursor: pointer; border-radius: 12px; overflow: hidden; transition: transform 0.2s; }
.activity-card:hover { transform: translateY(-4px); }
.card-image { position: relative; }
.image-placeholder { height: 200px; display: flex; align-items: center; justify-content: center; background: #f0f0f0; font-size: 32px; color: #ccc; }
.card-status {
  position: absolute; top: 12px; right: 12px;
  padding: 4px 12px; border-radius: 20px; font-size: 12px; color: #fff;
}
.status-0 { background: #909399; }
.status-1 { background: #409eff; }
.status-2 { background: #e74c3c; animation: pulse 1.5s infinite; }
.status-3 { background: #909399; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.7; } }
.card-body { padding: 16px; }
.card-title { font-size: 16px; margin-bottom: 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.card-price { display: flex; align-items: baseline; gap: 8px; margin-bottom: 12px; }
.flash-price { font-size: 24px; font-weight: bold; color: #e74c3c; }
.original-price { font-size: 14px; color: #999; text-decoration: line-through; }
.card-progress { margin-bottom: 8px; }
.card-stats { display: flex; justify-content: space-between; font-size: 13px; color: #666; }
.countdown { color: #e74c3c; font-weight: bold; }
/* 搜索结果高亮 */
:deep(.search-highlight) { color: #e74c3c; font-weight: bold; background: #fff3cd; padding: 0 2px; border-radius: 2px; }
.pagination { text-align: center; margin-top: 24px; }
</style>
