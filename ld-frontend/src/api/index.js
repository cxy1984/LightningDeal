import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 是否正在刷新 token 的标记
let isRefreshing = false
let refreshSubscribers = []

function onRefreshed(newToken) {
  refreshSubscribers.forEach(cb => cb(newToken))
  refreshSubscribers = []
}

function addRefreshSubscriber(cb) {
  refreshSubscribers.push(cb)
}

// 请求拦截器 - 添加 accessToken
request.interceptors.request.use(
  config => {
    const accessToken = localStorage.getItem('accessToken')
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - 统一处理错误 + 自动刷新 token
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return res
  },
  async error => {
    const { config, response } = error
    // 401 且非刷新接口本身 → 尝试刷新 token
    if (response?.status === 401 && !config?.url?.includes('/auth/refresh') && !config?.url?.includes('/user/login')) {
      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        // 没有 refreshToken，跳登录
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        window.location.href = '/login'
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(error)
      }

      if (!isRefreshing) {
        isRefreshing = true
        try {
          const res = await axios.post('/api/auth/refresh', { refreshToken })
          const { accessToken: newAccess, refreshToken: newRefresh } = res.data.data
          localStorage.setItem('accessToken', newAccess)
          localStorage.setItem('refreshToken', newRefresh)
          config.headers.Authorization = `Bearer ${newAccess}`
          isRefreshing = false
          onRefreshed(newAccess)
          // 重试原请求
          return request(config)
        } catch (refreshError) {
          isRefreshing = false
          refreshSubscribers = []
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          window.location.href = '/login'
          ElMessage.error('登录已过期，请重新登录')
          return Promise.reject(refreshError)
        }
      } else {
        // 正在刷新，排队等待
        return new Promise(resolve => {
          addRefreshSubscriber(newToken => {
            config.headers.Authorization = `Bearer ${newToken}`
            resolve(request(config))
          })
        })
      }
    }

    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，网络似乎不太好，请重试')
    } else if (response?.status >= 500) {
      ElMessage.error('服务器繁忙，请稍后再试')
    } else if (error.message === 'Network Error') {
      ElMessage.error('网络连接失败，请检查网络')
    } else if (response?.status === 429) {
      if (!config?.url?.includes('/seckill/')) {
        ElMessage.warning('操作太频繁，请稍后再试')
      }
    } else if (response?.status !== 401) {
      // 非 401 的错误才提示（401 已经处理）
      const msg = response?.data?.msg || error.message || '网络错误'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

// ===== API 接口 =====
export const api = {
  // 用户
  login: data => request.post('/user/login', data),
  register: data => request.post('/user/register', data),
  getUserInfo: () => request.get('/user/info'),
  refreshToken: data => request.post('/auth/refresh', data),

  // 活动
  getActivityList: params => request.get('/activity/list', { params }),
  getActivityDetail: id => request.get(`/activity/detail/${id}`),
  createActivity: data => request.post('/activity/create', data),
  updateActivity: data => request.put('/activity/update', data),
  deleteActivity: id => request.delete(`/activity/${id}`),
  updateActivityStatus: (id, status) => request.put(`/activity/status/${id}`, null, { params: { status } }),
  preheatStock: id => request.post(`/activity/preheat/${id}`),

  // 秒杀
  executeSeckill: data => request.post('/seckill/execute', data),
  getSeckillResult: activityId => request.get(`/seckill/result/${activityId}`),

  // 订单
  getOrders: params => request.get('/order/list', { params }),
  getOrderDetail: orderId => request.get(`/order/detail/${orderId}`),
  payOrder: orderId => request.post(`/order/pay/${orderId}`),
  cancelOrder: orderId => request.post(`/order/cancel/${orderId}`),
  refundOrder: orderId => request.post(`/order/refund/${orderId}`),

  // 搜索
  searchActivity: params => request.get('/search/activity', { params }),
  syncSearch: () => request.post('/search/sync'),

  // 用户中心
  getUserInfo: () => request.get('/user/info'),
  updateProfile: data => request.put('/user/profile', data),
  updatePassword: data => request.put('/user/password', data),

  // 大屏
  getDashboardData: () => request.get('/dashboard/data'),
  getQps: () => request.get('/dashboard/qps'),

  // 文件
  uploadFile: formData => request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export default request
