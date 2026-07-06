import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useWebSocketStore = defineStore('websocket', () => {
  const seckillWs = ref(null)
  const dashboardWs = ref(null)

  // 秒杀结果回调列表
  const seckillCallbacks = []

  /**
   * 注册秒杀结果回调
   * @param {Function} callback - 接收 (result) => void
   * @returns {Function} 取消注册的函数
   */
  function onSeckillResult(callback) {
    seckillCallbacks.push(callback)
    return () => {
      const idx = seckillCallbacks.indexOf(callback)
      if (idx !== -1) seckillCallbacks.splice(idx, 1)
    }
  }

  function connectSeckill(userId) {
    if (seckillWs.value) return
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/seckill/result/${userId}`
    seckillWs.value = new WebSocket(wsUrl)

    seckillWs.value.onmessage = (event) => {
      try {
        const result = JSON.parse(event.data)
        // 通知所有注册的回调
        seckillCallbacks.forEach(cb => cb(result))
      } catch (e) {
        // ignore parse error
      }
    }

    seckillWs.value.onclose = () => {
      seckillWs.value = null
    }

    seckillWs.value.onerror = () => {
      seckillWs.value = null
    }

    // 心跳
    const heartbeat = setInterval(() => {
      if (seckillWs.value?.readyState === WebSocket.OPEN) {
        seckillWs.value.send('ping')
      }
    }, 30000)

    // 页面关闭时清理心跳
    window.addEventListener('beforeunload', () => clearInterval(heartbeat), { once: true })
  }

  function connectDashboard(onMessage) {
    if (dashboardWs.value) return
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/dashboard`
    dashboardWs.value = new WebSocket(wsUrl)

    dashboardWs.value.onmessage = (event) => {
      try {
        onMessage?.(JSON.parse(event.data))
      } catch (e) {
        // ignore
      }
    }

    dashboardWs.value.onclose = () => {
      dashboardWs.value = null
    }

    dashboardWs.value.onerror = () => {
      dashboardWs.value = null
    }
  }

  function disconnectAll() {
    seckillWs.value?.close()
    dashboardWs.value?.close()
    seckillWs.value = null
    dashboardWs.value = null
    seckillCallbacks.length = 0
  }

  return { seckillWs, dashboardWs, onSeckillResult, connectSeckill, connectDashboard, disconnectAll }
})
