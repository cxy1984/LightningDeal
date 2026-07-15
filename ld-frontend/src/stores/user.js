import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/api'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const role = ref(localStorage.getItem('role') || '')

  async function login(loginData) {
    const res = await api.login(loginData)
    token.value = res.data.accessToken
    refreshToken.value = res.data.refreshToken
    role.value = res.data.role || ''
    localStorage.setItem('accessToken', res.data.accessToken)
    localStorage.setItem('refreshToken', res.data.refreshToken)
    localStorage.setItem('role', res.data.role || '')
    await fetchUserInfo()
    return res
  }

  async function register(registerData) {
    return await api.register(registerData)
  }

  async function fetchUserInfo() {
    try {
      const res = await api.getUserInfo()
      user.value = res.data
    } catch (e) {
      // ignore
    }
  }

  function logout() {
    user.value = null
    token.value = ''
    refreshToken.value = ''
    role.value = ''
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('role')
  }

  function setTokens(access, refresh) {
    token.value = access
    refreshToken.value = refresh
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
  }

  return { user, token, refreshToken, role, login, register, fetchUserInfo, logout, setTokens }
})
