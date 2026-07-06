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

          <!-- 秒杀结果展示（内嵌） -->
          <div v-if="myResult" class="my-result-card" :class="'result-' + myResult.status">
            <div class="result-icon">{{ resultIcon }}</div>
            <div class="result-body">
              <p class="result-title">{{ myResult.message }}</p>
              <p class="result-sub" v-if="myResult.status === 1">正在处理，请稍候...</p>
            </div>
            <div class="result-actions">
              <el-button v-if="myResult.status === 2 && myResult.orderId" type="primary" size="small" @click="goToOrder">
                查看订单
              </el-button>
              <el-button v-if="myResult.status === 5" type="danger" size="small" @click="retrySeckill" :disabled="seckillDisabled || seckilling">
                再试一次
              </el-button>
            </div>
          </div>
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
const flashStream = ref([])
const myResult = ref(null)

// 秒杀结果状态
const RESULT_QUEUING = 1
const RESULT_SUCCESS = 2
const RESULT_FAIL = 3
const RESULT_REPEAT = 4
const RESULT_LIMITED = 5

let countdownTimer = null
let pollingTimer = null
let unsubscribeWs = null

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

const resultIcon = computed(() => {
  const icons = { 1: '⏳', 2: '🎉', 3: '😢', 4: 'ℹ️', 5: '😤' }
  return icons[myResult.value?.status] || ''
})

const countdownDisplay = ref('')

onMounted(async () => {
  await fetchDetail()
  startCountdown()
  setupWebSocket()
  // 恢复上次未完成的秒杀结果查询
  const pendingAid = activity.value?.id
  if (pendingAid && localStorage.getItem(`seckill_pending_${pendingAid}`)) {
    startPolling(pendingAid)
  }
})

onUnmounted(() => {
  clearInterval(countdownTimer)
  clearTimeout(pollingTimer)
  if (unsubscribeWs) unsubscribeWs()
  localStorage.removeItem('seckill_pending_' + activity.value?.id)
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

function setupWebSocket() {
  if (userStore.token && userStore.user?.id) {
    wsStore.connectSeckill(userStore.user.id)
    unsubscribeWs = wsStore.onSeckillResult((result) => {
      if (result.activityId === activity.value?.id) {
        handleResult(result)
      }
    })
  }
}

function handleResult(result) {
  // 后端状态 → 前端状态
  // 1=排队中, 2=成功, 3=失败, 4=重复参与
  const statusMap = { 1: RESULT_QUEUING, 2: RESULT_SUCCESS, 3: RESULT_FAIL, 4: RESULT_REPEAT }
  myResult.value = {
    status: statusMap[result.status] || RESULT_FAIL,
    message: result.message,
    orderId: result.orderId || null
  }

  // 更新成交记录
  flashStream.value.unshift({
    username: '我',
    success: result.success,
    timestamp: Date.now()
  })
  if (flashStream.value.length > 50) flashStream.value.length = 50

  // 清理轮询
  localStorage.removeItem(`seckill_pending_${activity.value?.id}`)
  clearTimeout(pollingTimer)
  seckilling.value = false
}

function startPolling(activityId) {
  const poll = () => {
    pollingTimer = setTimeout(async () => {
      try {
        const res = await api.getSeckillResult(activityId)
        const result = res.data
        if (result.status === 1) {
          startPolling(activityId)
        } else {
          handleResult(result)
        }
      } catch (e) {
        startPolling(activityId)
      }
    }, 2000)
  }
  poll()
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
      // 排队中
      myResult.value = { status: RESULT_QUEUING, message: '正在排队处理，请稍候...', orderId: null }
      localStorage.setItem(`seckill_pending_${activity.value.id}`, '1')
      // 5 秒后轮询兜底
      setTimeout(() => {
        if (seckilling.value) startPolling(activity.value.id)
      }, 5000)
    } else {
      handleResult(result)
    }
  } catch (e) {
    // 限流或其他异常（RateLimitAspect 返回 429）
    const msg = e?.message || '系统繁忙'
    myResult.value = { status: RESULT_LIMITED, message: msg, orderId: null }
    seckilling.value = false
  }
}

function retrySeckill() {
  myResult.value = null
  handleSeckill()
}

function goToOrder() {
  if (myResult.value?.orderId) {
    router.push(`/order/detail/${myResult.value.orderId}`)
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

/* 内嵌结果卡片 */
.my-result-card {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid;
}
.result-icon { font-size: 36px; flex-shrink: 0; }
.result-body { flex: 1; min-width: 0; }
.result-title { margin: 0; font-weight: bold; font-size: 16px; }
.result-sub { margin: 4px 0 0; font-size: 13px; color: #666; }
.result-actions { flex-shrink: 0; }
.result-1 { background: #fff3cd; border-color: #ffc107; }
.result-2 { background: #d4edda; border-color: #28a745; }
.result-3 { background: #f8d7da; border-color: #dc3545; }
.result-4 { background: #d1ecf1; border-color: #17a2b8; }
.result-5 { background: #f8d7da; border-color: #e74c3c; }

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
