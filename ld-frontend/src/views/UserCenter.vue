<template>
  <div class="user-center">
    <el-card class="profile-card">
      <template #header>
        <span>👤 个人资料</span>
      </template>
      <el-form label-width="100px" :model="form" :rules="rules" ref="profileFormRef">
        <el-form-item label="用户名">
          <span class="readonly-field">{{ userStore.user?.username }}</span>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleUpdateProfile" :loading="profileLoading">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="password-card">
      <template #header>
        <span>🔒 修改密码</span>
      </template>
      <el-form label-width="100px" :model="pwdForm" :rules="pwdRules" ref="pwdFormRef">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="handleUpdatePassword" :loading="pwdLoading">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { api } from '@/api'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const profileFormRef = ref(null)
const pwdFormRef = ref(null)
const profileLoading = ref(false)
const pwdLoading = ref(false)

const form = reactive({
  nickname: '',
  phone: '',
  email: ''
})

const rules = {
  nickname: [{ required: true, message: '昵称不能为空', trigger: 'blur' }]
}

const pwdForm = reactive({
  oldPassword: '',
  newPassword: ''
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

onMounted(() => {
  // 填充当前用户信息
  if (userStore.user) {
    form.nickname = userStore.user.nickname || ''
    form.phone = userStore.user.phone || ''
    form.email = userStore.user.email || ''
  } else {
    // 未加载则获取
    loadUserInfo()
  }
})

async function loadUserInfo() {
  try {
    const res = await api.getUserInfo()
    userStore.user = res.data
    form.nickname = res.data.nickname || ''
    form.phone = res.data.phone || ''
    form.email = res.data.email || ''
  } catch {
    // ignore
  }
}

async function handleUpdateProfile() {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return
  profileLoading.value = true
  try {
    const res = await api.updateProfile({
      nickname: form.nickname,
      phone: form.phone || null,
      email: form.email || null
    })
    userStore.user = res.data
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    profileLoading.value = false
  }
}

async function handleUpdatePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdLoading.value = true
  try {
    await api.updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
  } catch {
    ElMessage.error('密码修改失败')
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.user-center { max-width: 600px; margin: 0 auto; }
.profile-card { margin-bottom: 24px; }
.password-card { margin-bottom: 24px; }
.readonly-field { color: #666; line-height: 32px; }
</style>
