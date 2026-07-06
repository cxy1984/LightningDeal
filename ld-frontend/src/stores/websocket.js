import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 单个 WebSocket 连接管理器（支持自动重连）
 */
class WSConnection {
  /**
   * @param {string} url
   * @param {Object} options
   * @param {Function} options.onMessage - 收到 JSON 消息的回调
   * @param {Function} options.onReconnect - 重连成功后的回调
   * @param {Function} options.onStateChange - 状态变化回调 (state: 'connecting'|'connected'|'disconnected') => void
   * @param {number} options.reconnectDelay - 首次重连延迟，默认 1s
   * @param {number} options.maxRetries - 最大重试次数，默认 10
   */
  constructor(url, options = {}) {
    this.url = url
    this.onMessage = options.onMessage || (() => {})
    this.onReconnect = options.onReconnect || (() => {})
    this.onStateChange = options.onStateChange || (() => {})
    this.reconnectDelay = options.reconnectDelay || 1000
    this.maxRetries = options.maxRetries || 10

    this.ws = null
    this.retryCount = 0
    this._heartbeatTimer = null
    this._reconnectTimer = null
    this._destroyed = false
    this._wasConnected = false

    this._connect()
  }

  _connect() {
    if (this._destroyed) return
    this._updateState('connecting')

    try {
      this.ws = new WebSocket(this.url)
    } catch {
      this._scheduleReconnect()
      return
    }

    this.ws.onopen = () => {
      this._updateState('connected')
      this.retryCount = 0
      this._startHeartbeat()
      if (this._wasConnected) {
        // 重连成功，通知上层拉取遗漏数据
        this.onReconnect()
      }
      this._wasConnected = true
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        this.onMessage(data)
      } catch {
        // 非 JSON 消息（如 pong）忽略
      }
    }

    this.ws.onclose = () => {
      this.ws = null
      this._stopHeartbeat()
      this._updateState('disconnected')
      this._scheduleReconnect()
    }

    this.ws.onerror = () => {
      // onerror 后必然触发 onclose，重连逻辑在 onclose 中处理
    }
  }

  _scheduleReconnect() {
    if (this._destroyed) return
    if (this.retryCount >= this.maxRetries) {
      this._updateState('disconnected')
      return
    }
    const delay = Math.min(
      this.reconnectDelay * Math.pow(2, this.retryCount),
      30000
    )
    this.retryCount++
    this._reconnectTimer = setTimeout(() => this._connect(), delay)
  }

  _startHeartbeat() {
    this._stopHeartbeat()
    this._heartbeatTimer = setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.ws.send('ping')
      }
    }, 30000)
  }

  _stopHeartbeat() {
    if (this._heartbeatTimer) {
      clearInterval(this._heartbeatTimer)
      this._heartbeatTimer = null
    }
  }

  _updateState(state) {
    this.onStateChange(state)
  }

  /** 主动断开，不再重连 */
  close() {
    this._destroyed = true
    if (this._reconnectTimer) {
      clearTimeout(this._reconnectTimer)
      this._reconnectTimer = null
    }
    this._stopHeartbeat()
    this.ws?.close()
    this.ws = null
    this._updateState('disconnected')
  }
}

export const useWebSocketStore = defineStore('websocket', () => {
  /** 后端 WebSocket 地址（注意 context-path /api） */
  const WS_BASE = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//localhost:8080/api`

  // 连接管理器实例
  let seckillConn = null
  let dashboardConn = null

  // 状态（响应式，组件可 watch）
  const seckillState = ref('disconnected')
  const dashboardState = ref('disconnected')

  // 秒杀结果回调列表
  const seckillCallbacks = []

  /**
   * 注册秒杀结果回调
   * @param {Function} callback - (result) => void
   * @returns {Function} 取消注册的函数
   */
  function onSeckillResult(callback) {
    seckillCallbacks.push(callback)
    return () => {
      const idx = seckillCallbacks.indexOf(callback)
      if (idx !== -1) seckillCallbacks.splice(idx, 1)
    }
  }

  /**
   * 连接秒杀结果 WebSocket
   * @param {number} userId
   * @param {Function} onReconnect - 重连成功后的回调（可选，用于拉取遗漏结果）
   */
  function connectSeckill(userId, onReconnect) {
    // 如果已经连到这个 userId，不重复创建
    if (seckillConn && seckillConn._userId === userId) return

    // 如果连了其他 userId，断开
    if (seckillConn) {
      seckillConn.close()
      seckillConn = null
    }

    const url = `${WS_BASE}/ws/seckill/result/${userId}`
    seckillConn = new WSConnection(url, {
      onMessage: (result) => {
        seckillCallbacks.forEach(cb => cb(result))
      },
      onReconnect: () => {
        // 重连后通知上层拉取遗漏数据
        onReconnect?.()
      },
      onStateChange: (state) => {
        seckillState.value = state
      }
    })
    seckillConn._userId = userId
  }

  /**
   * 连接仪表盘 WebSocket
   */
  function connectDashboard(onMessage) {
    if (dashboardConn) return
    const url = `${WS_BASE}/ws/dashboard`
    dashboardConn = new WSConnection(url, {
      onMessage: (data) => {
        onMessage?.(data)
      },
      onStateChange: (state) => {
        dashboardState.value = state
      }
    })
  }

  /** 断开所有连接 */
  function disconnectAll() {
    seckillConn?.close()
    dashboardConn?.close()
    seckillConn = null
    dashboardConn = null
    seckillCallbacks.length = 0
    seckillState.value = 'disconnected'
    dashboardState.value = 'disconnected'
  }

  return {
    seckillState,
    dashboardState,
    onSeckillResult,
    connectSeckill,
    connectDashboard,
    disconnectAll
  }
})
