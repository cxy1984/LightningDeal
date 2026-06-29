<template>
  <div class="detail-page" v-if="activity">
    <el-row :gutter="32">
      <!-- 商品图 -->
      <el-col :xs="24" :md="10">
        <div class="detail-image">
          <el-image :src="activity.goodsImage || '/placeholder.png'" fit="cover" style="width:100%;height:400px;border-radius:12px">
            <template #error>
              <div class="img-ph">📷 {{ activity.goodsName }}</div>
            </template>
          </el-image>
        </div>
      </el-col>
      <!-- 商品信息 -->
      <el-col :xs="24" :md="14">
        <div class="detail-info">
          <h1 class="detail-title">{{ activity.name }}</h1>
          <p class="detail-desc">{{ activity.goodsDescription || '暂无描述' }}</p>

          <div class="price-section">
            <div class="flash-price">¥{{ activity.flashPrice }}</div>
            <div class="original-price">原价 ¥{{ activity.originalPrice }}</div>
            <div class="discount">{{ discountText }}折</div>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <span class="label">库存</span>
              <span class="value">{{ activity.remainStock }} / {{ activity.totalStock }}</span>
            </div>
            <div class="info-item">
              <span class="label">已抢</span>
              <span class="value">{{ activity.soldStock }}</span>
            </div>
            <div class="info-item">
              <span class="label">限购</span>
              <span class="value">{{ activity.limitPerUser }} 件/人</span>
            </div>
            <div class="info-item">
              <span class="label">状态</span>
              <span class="value" :class="'status-' + activity.status">{{ statusText }}</span>
            </div>
          </div>

          <!-- 倒计时 -->
          <div class="countdown-section" v-if="activity.countingDown">
            <span class="countdown-label">距开始</span>
            <span class="countdown-timer">{{ countdownDisplay }}</span>
          </div>

          <!-- 抢购按钮 -->
          <div class="action-section">
            <el-button
              :type="seckillBtnType"
              size="large"
              :disabled="seckillDisabled"
              :loading="seckilling"
              class="seckill-btn"
              @click="handleSeckill"
            >
              {{ seckillBtnText }}
            </el-button>
            <el-button @click="$router.push('/activity')" size="large">返回列表</el-button>
          </div>

          <!-- 结果弹窗 -->
          <el-dialog v-model="resultVisible" title="抢购结果" width="360px" :close-on-click-modal="false">
            <div class="result-content">
              <div :class="['result-icon', seckillResult?.success ? 'success' : 'fail']">
                {{ seckillResult?.success ? '🎉' : '😢' }}
              </div>
              <p class="result-msg">{{ seckillResult?.message }}</p>
              <p v-if="seckillResult?.status === 1" class="result-hint">正在排队处理，请稍候...</p>
            </div>
            <template #footer>
              <el-button @click="resultVisible = false" type="primary">知道了</el-button>
            </template>
          </el-dialog>
        </div>
      </el-col>
    </el-row>

    <!-- 实时成交记录 -->
    <el-card class="flash-stream" title="⚡ 实时成交">
      <div class="stream-container">
        <div class="stream-item" v-for="(item, i) in flashStream" :key="i">
          <span class="stream-user">{{ item.username }}</span>
          <span :class="item.success ? 'stream-success' : 'stream-fail'">
            {{ item.success ? '✅ 抢到' : '❌ 未抢到' }}
          </span>
          <span class="stream-time">{{ formatTime(item.timestamp) }}</span>
        </div>
        <el-empty v-if="!flashStream.length" description="暂无成交记录" :image-size="60" />
      </div>
    </el-card>
  </div>

  <div v-else class="loading">
    <el-skeleton :rows="10" animated />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api'
import { useUserStore } from '@/stores/user'
import { useWebSocketStore } from '@/stores/websocket'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const wsStore = useWebSocketStore()

const activity = ref(null)
const seckilling = ref(false)
const resultVisible = ref(false)
const seckillResult = ref(null)
const flashStream = ref([])
let countdownTimer = null

const discountText = computed(() => {
  if (!activity.value) return ''
  const d = activity.value.flashPrice / activity.value.originalPrice * 10
  return d.toFixed(1)
})

const statusText = computed(() => {
  const s = activity.value?.status
  return ['草稿', '即将开始', '抢购中', '已结束'][s] || '未知'
})

const seckillBtnType = computed(() => {
  const s = activity.value?.status
  if (s === 2) return 'danger'
  if (s === 1) return 'warning'
  return 'info'
})

const seckillDisabled = computed(() => {
  const s = activity.value?.status
  return s !== 2 || !userStore.token
})

const seckillBtnText = computed(() => {
  if (!userStore.token) return '请先登录'
  const s = activity.value?.status
  if (s === 1) return '即将开始'
  if (s === 2) return '⚡ 立即抢购'
  if (s === 3) return '已结束'
  return '暂无活动'
})

const countdownDisplay = ref('')

onMounted(async () => {
  await fetchDetail()
  startCountdown()
  connectWebSocket()
})

onUnmounted(() => {
  clearInterval(countdownTimer)
})

async function fetchDetail() {
  const id = route.params.id
  const res = await api.getActivityDetail(id)
  activity.value = res.data
}

function startCountdown() {
  countdownTimer = setInterval(() => {
    if (activity.value?.countingDown) {
      const remain = activity.value.countdownMillis - 1000
      if (remain <= 0) {
        activity.value.status = 2
        activity.value.countdownMillis = 0
        activity.value.countingDown = false
      } else {
        activity.value.countdownMillis = remain
      }
      countdownDisplay.value = formatCountdown(activity.value.countdownMillis)
    }
  }, 1000)
}

function connectWebSocket() {
  if (userStore.token) {
    wsStore.connectSeckill(userStore.user?.id)
    // 监听秒杀结果
    const unwatch = wsStore.$subscribe((mutation, state) => {
      if (state.lastMessage) {
        handleResult(state.lastMessage)
        state.lastMessage = null
      }
    })
    onUnmounted(unwatch)
  }
}

function handleResult(result) {
  if (result.activityId === activity.value?.id) {
    seckillResult.value = result
    resultVisible.value = true
    seckilling.value = false
    // 更新成交记录
    flashStream.value.unshift({
      username: result.success ? '我' : '我',
      success: result.success,
      timestamp: Date.now()
    })
    if (flashStream.value.length > 50) flashStream.value.length = 50
  }
}

async function handleSeckill() {
  if (!userStore.token) {
    router.push('/login')
    return
  }
  seckilling.value = true
  try {
    const res = await api.executeSeckill({ activityId: activity.value.id, quantity: 1 })
    const result = res.data
    if (result.status === 1) {
      // 排队中，等待 WebSocket 推送
      ElMessage.info('正在排队处理，请稍候...')
    } else {
      handleResult(result)
    }
  } catch (e) {
    seckilling.value = false
  }
}

function formatCountdown(ms) {
  if (ms <= 0) return '00:00:00'
  const totalSeconds = Math.floor(ms / 1000)
  const h = Math.floor(totalSeconds / 3600)
  const m = Math.floor((totalSeconds % 3600) / 60)
  const s = totalSeconds % 60
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

function formatTime(ts) {
  const d = new Date(ts)
  return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}:${d.getSeconds().toString().padStart(2,'0')}`
}
</script>

<style scoped>
.detail-page { max-width: 1200px; margin: 0 auto; }
.loading { max-width: 1200px; margin: 0 auto; padding: 40px; }
.detail-image { margin-bottom: 20px; }
.img-ph { height: 400px; display: flex; align-items: center; justify-content: center; background: #f0f0f0; font-size: 48px; }
.detail-title { font-size: 28px; margin-bottom: 8px; }
.detail-desc { color: #666; margin-bottom: 24px; }
.price-section { display: flex; align-items: baseline; gap: 12px; margin-bottom: 24px; padding: 16px; background: #fff5f5; border-radius: 8px; }
.flash-price { font-size: 36px; font-weight: bold; color: #e74c3c; }
.original-price { font-size: 16px; color: #999; text-decoration: line-through; }
.discount { padding: 2px 8px; background: #e74c3c; color: #fff; border-radius: 4px; font-size: 13px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px; }
.info-item { padding: 12px 16px; background: #f9f9f9; border-radius: 8px; }
.info-item .label { display: block; font-size: 12px; color: #999; margin-bottom: 4px; }
.info-item .value { font-size: 18px; font-weight: bold; }
.countdown-section { text-align: center; padding: 20px; background: #fff3cd; border-radius: 8px; margin-bottom: 24px; }
.countdown-label { font-size: 16px; margin-right: 12px; }
.countdown-timer { font-size: 36px; font-weight: bold; color: #e74c3c; letter-spacing: 4px; }
.action-section { display: flex; gap: 16px; }
.seckill-btn { flex: 1; font-size: 18px; height: 56px; }
.result-content { text-align: center; padding: 20px; }
.result-icon { font-size: 64px; margin-bottom: 16px; }
.result-msg { font-size: 18px; font-weight: bold; }
.result-hint { font-size: 14px; color: #999; margin-top: 8px; }
.flash-stream { margin-top: 32px; }
.stream-container { max-height: 300px; overflow-y: auto; }
.stream-item { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.stream-user { font-weight: bold; }
.stream-success { color: #67c23a; }
.stream-fail { color: #e74c3c; }
.stream-time { color: #999; font-size: 12px; }
.status-1 { color: #409eff; }
.status-2 { color: #e74c3c; }
.status-3 { color: #999; }
</style>
