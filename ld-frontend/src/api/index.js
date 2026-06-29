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
    } else {
      ElMessage.error(error.message || '网络错误')
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
  preheatStock: id => request.post(`/activity/preheat/${id}`),

  // 秒杀
  executeSeckill: data => request.post('/seckill/execute', data),
  getSeckillResult: activityId => request.get(`/seckill/result/${activityId}`),

  // 订单
  getOrders: params => request.get('/order/list', { params }),
  payOrder: orderId => request.post(`/order/pay/${orderId}`),
  cancelOrder: orderId => request.post(`/order/cancel/${orderId}`),

  // 搜索
  searchActivity: params => request.get('/search/activity', { params }),

  // 大屏
  getDashboardData: () => request.get('/dashboard/data'),
  getQps: () => request.get('/dashboard/qps'),

  // 文件
  uploadFile: formData => request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export default request
