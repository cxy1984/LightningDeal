import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器 - 添加 Token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - 统一处理错误
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return res
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
      ElMessage.error('登录已过期，请重新登录')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，网络似乎不太好，请重试')
    } else if (error.response?.status >= 500) {
      ElMessage.error('服务器繁忙，请稍后再试')
    } else if (error.message === 'Network Error') {
      ElMessage.error('网络连接失败，请检查网络')
    } else if (error.response?.status === 429) {
      // 限流错误在秒杀页面已有弹窗处理，全局只提示，不覆盖
      if (!error.config?.url?.includes('/seckill/')) {
        ElMessage.warning('操作太频繁，请稍后再试')
      }
    } else {
      const msg = error.response?.data?.msg || error.message || '网络错误'
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
