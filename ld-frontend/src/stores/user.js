import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/api'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  async function login(loginData) {
    const res = await api.login(loginData)
    token.value = res.data
    localStorage.setItem('token', res.data)
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
    localStorage.removeItem('token')
  }

  return { user, token, login, register, fetchUserInfo, logout }
})
