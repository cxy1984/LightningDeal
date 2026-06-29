<template>
  <div class="dashboard-page">
    <h2 class="page-title">📊 实时监控大屏</h2>

    <!-- 系统状态 -->
    <el-row :gutter="16" class="status-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <span class="stat-label">当前 QPS</span>
            <span class="stat-value">{{ data.currentQps }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <span class="stat-label">峰值 QPS</span>
            <span class="stat-value peak">{{ data.peakQps }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <span class="stat-label">成功订单</span>
            <span class="stat-value success">{{ data.successOrders }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <span class="stat-label">失败订单</span>
            <span class="stat-value fail">{{ data.failOrders }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <!-- QPS 曲线 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>📈 QPS 实时曲线</template>
          <div ref="qpsChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <!-- 商品排行榜 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>🏆 商品 TOP 排行榜</template>
          <div class="rank-list">
            <div v-for="(item, i) in data.rankList" :key="i" class="rank-item">
              <span class="rank-num" :class="'rank-' + (i+1)">{{ i + 1 }}</span>
              <span class="rank-name">{{ item.goodsName }}</span>
              <span class="rank-sales">{{ item.salesCount }} 单</span>
            </div>
            <el-empty v-if="!data.rankList.length" description="暂无数据" :image-size="40" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <!-- 实时抢购流水 -->
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>⚡ 实时抢购流水</template>
          <div class="stream-container">
            <div class="stream-header">
              <span>用户</span><span>商品</span><span>结果</span><span>时间</span>
            </div>
            <div class="stream-body">
              <div v-for="(item, i) in data.flashStream" :key="i" class="stream-row">
                <span>{{ item.username }}</span>
                <span>{{ item.goodsName }}</span>
                <span :class="item.success ? 'txt-success' : 'txt-fail'">
                  {{ item.success ? '✅ 成功' : '❌ 失败' }}
                </span>
                <span class="txt-time">{{ formatTime(item.timestamp) }}</span>
              </div>
              <el-empty v-if="!data.flashStream.length" description="暂无抢购流水" :image-size="40" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { api } from '@/api'
import { useWebSocketStore } from '@/stores/websocket'
import * as echarts from 'echarts'

const wsStore = useWebSocketStore()
const qpsChartRef = ref(null)
let qpsChart = null
let refreshTimer = null

const data = ref({
  currentQps: 0,
  peakQps: 0,
  totalOrders: 0,
  successOrders: 0,
  failOrders: 0,
  qpsHistory: [],
  flashStream: [],
  rankList: [],
  systemStatus: '🟢 正常'
})

onMounted(async () => {
  await fetchData()
  initChart()
  startPolling()
  connectWs()
})

onUnmounted(() => {
  clearInterval(refreshTimer)
  qpsChart?.dispose()
  wsStore.disconnectAll()
})

async function fetchData() {
  try {
    const res = await api.getDashboardData()
    data.value = res.data
    updateChart()
  } catch (e) { /* ignore */ }
}

function startPolling() {
  refreshTimer = setInterval(fetchData, 3000)
}

function connectWs() {
  wsStore.connectDashboard((msg) => {
    // 实时推送的大屏数据更新
    if (msg.currentQps !== undefined) {
      Object.assign(data.value, msg)
      updateChart()
    }
  })
}

function initChart() {
  nextTick(() => {
    if (!qpsChartRef.value) return
    qpsChart = echarts.init(qpsChartRef.value)
    const option = {
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: Array.from({ length: 60 }, (_, i) => (i + 1) + 's'),
        axisLabel: { fontSize: 10, interval: 10 }
      },
      yAxis: { type: 'value', name: 'QPS' },
      series: [{
        name: 'QPS',
        type: 'line',
        smooth: true,
        symbol: 'none',
        lineStyle: { color: '#e74c3c', width: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(231,76,60,0.3)' },
            { offset: 1, color: 'rgba(231,76,60,0.05)' }
          ])
        },
        data: []
      }]
    }
    qpsChart.setOption(option)
  })
}

function updateChart() {
  if (!qpsChart) return
  const qpsData = data.value.qpsHistory || Array(60).fill(0)
  qpsChart.setOption({
    series: [{ data: qpsData.slice(-60) }]
  })
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}:${d.getSeconds().toString().padStart(2,'0')}`
}
</script>

<style scoped>
.dashboard-page { max-width: 1400px; margin: 0 auto; }
.page-title { margin-bottom: 20px; }
.status-row { margin-bottom: 16px; }
.stat-item { text-align: center; padding: 8px 0; }
.stat-label { display: block; font-size: 14px; color: #999; margin-bottom: 8px; }
.stat-value { font-size: 32px; font-weight: bold; color: #333; }
.stat-value.peak { color: #e74c3c; }
.stat-value.success { color: #67c23a; }
.stat-value.fail { color: #e74c3c; }
.chart-row { margin-bottom: 16px; }
.rank-list { min-height: 200px; }
.rank-item { display: flex; align-items: center; padding: 8px 0; border-bottom: 1px solid #f5f5f5; gap: 12px; }
.rank-num { width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: bold; color: #fff; background: #909399; }
.rank-1 { background: #e74c3c; }
.rank-2 { background: #e67e22; }
.rank-3 { background: #f1c40f; }
.rank-name { flex: 1; font-size: 14px; }
.rank-sales { font-size: 13px; color: #e74c3c; font-weight: bold; }
.stream-container { max-height: 300px; overflow-y: auto; }
.stream-header, .stream-row { display: grid; grid-template-columns: 1fr 2fr 1fr 1fr; padding: 8px 12px; gap: 8px; }
.stream-header { font-weight: bold; background: #f9f9f9; border-bottom: 2px solid #eee; }
.stream-row { border-bottom: 1px solid #f5f5f5; font-size: 13px; }
.stream-row:hover { background: #fafafa; }
.txt-success { color: #67c23a; }
.txt-fail { color: #e74c3c; }
.txt-time { color: #999; font-size: 12px; }
</style>
