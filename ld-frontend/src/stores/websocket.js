import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useWebSocketStore = defineStore('websocket', () => {
  const seckillWs = ref(null)
  const dashboardWs = ref(null)
  const lastMessage = ref(null)

  function connectSeckill(userId) {
    if (seckillWs.value) return
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/seckill/result/${userId}`
    seckillWs.value = new WebSocket(wsUrl)

    seckillWs.value.onmessage = (event) => {
      lastMessage.value = JSON.parse(event.data)
    }

    seckillWs.value.onclose = () => {
      seckillWs.value = null
    }

    // 心跳
    setInterval(() => {
      if (seckillWs.value?.readyState === WebSocket.OPEN) {
        seckillWs.value.send('ping')
      }
    }, 30000)
  }

  function connectDashboard(onMessage) {
    if (dashboardWs.value) return
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/dashboard`
    dashboardWs.value = new WebSocket(wsUrl)

    dashboardWs.value.onmessage = (event) => {
      onMessage?.(JSON.parse(event.data))
    }

    dashboardWs.value.onclose = () => {
      dashboardWs.value = null
    }
  }

  function disconnectAll() {
    seckillWs.value?.close()
    dashboardWs.value?.close()
    seckillWs.value = null
    dashboardWs.value = null
  }

  return { seckillWs, dashboardWs, lastMessage, connectSeckill, connectDashboard, disconnectAll }
})
