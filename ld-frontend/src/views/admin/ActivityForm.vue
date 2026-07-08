<template>
  <div class="form-page">
    <h2 class="page-title">{{ isEdit ? '编辑活动' : '创建活动' }}</h2>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 700px" v-loading="formLoading">
      <el-alert
        v-if="isEdit && activityStatus >= 2"
        type="warning"
        :closable="false"
        show-icon
        title="活动已开始，无法修改"
        description="该活动已经开始或已结束，只能查看详情"
        style="margin-bottom: 20px"
      />
      <el-alert
        v-if="isEdit && activityStatus >= 1 && activityStatus < 2"
        type="info"
        :closable="false"
        show-icon
        title="活动已上架"
        description="该活动已上架，修改后需重新同步到搜索"
        style="margin-bottom: 20px"
      />

      <el-form-item label="活动名称" prop="name">
        <el-input v-model="form.name" placeholder="例：iPhone 16 限时秒杀" :disabled="activityStatus >= 2" />
      </el-form-item>

      <el-form-item label="商品名称" prop="goodsName">
        <el-input v-model="form.goodsName" placeholder="例：iPhone 16 256GB" :disabled="activityStatus >= 2" />
      </el-form-item>

      <el-form-item label="商品描述" prop="goodsDescription">
        <el-input v-model="form.goodsDescription" type="textarea" :rows="3" placeholder="商品描述信息" :disabled="activityStatus >= 2" />
      </el-form-item>

      <el-form-item label="商品图片" prop="goodsImage">
        <div class="upload-wrap">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            list-type="picture-card"
            :file-list="fileList"
            :limit="1"
            :on-exceed="() => ElMessage.warning('只能上传一张图片')"
            :on-remove="handleRemove"
            :disabled="activityStatus >= 2"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议尺寸 800x800，支持 JPG/PNG，最大 5MB</div>
          <el-image
            v-if="form.goodsImage && fileList.length === 0"
            :src="form.goodsImage"
            style="width: 148px; height: 148px; border-radius: 8px; margin-top: 8px"
            fit="cover"
          />
        </div>
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="原价" prop="originalPrice">
            <el-input v-model.number="form.originalPrice" type="number" min="0" step="0.01" placeholder="0.00" :disabled="activityStatus >= 2">
              <template #prefix>¥</template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="秒杀价" prop="flashPrice">
            <el-input v-model.number="form.flashPrice" type="number" min="0" step="0.01" placeholder="0.00" :disabled="activityStatus >= 2">
              <template #prefix>¥</template>
            </el-input>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="总库存" prop="totalStock">
            <el-input v-model.number="form.totalStock" type="number" min="1" placeholder="100" :disabled="activityStatus >= 2" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="限购数量" prop="limitPerUser">
            <el-input v-model.number="form.limitPerUser" type="number" min="1" placeholder="1" :disabled="activityStatus >= 2" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker
          v-model="form.startTime"
          type="datetime"
          placeholder="选择开始时间"
          style="width: 100%"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DDTHH:mm:ss"
          :disabled="activityStatus >= 1"
        />
      </el-form-item>

      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker
          v-model="form.endTime"
          type="datetime"
          placeholder="选择结束时间"
          style="width: 100%"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DDTHH:mm:ss"
          :disabled="activityStatus >= 1"
        />
      </el-form-item>

      <el-form-item v-if="isEdit" label="活动状态">
        <el-tag :type="statusTagType">{{ statusTagText }}</el-tag>
      </el-form-item>

      <el-form-item>
        <template v-if="activityStatus < 2">
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '创建活动' }}
          </el-button>
        </template>
        <el-button size="large" @click="$router.push('/admin/activity')">
          {{ activityStatus >= 2 ? '返回' : '取消' }}
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const formLoading = ref(false)
const fileList = ref([])
const activityStatus = ref(0)

const isEdit = computed(() => !!route.params.id)

const uploadUrl = `${window.location.origin}/api/file/upload`
const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + localStorage.getItem('token')
}))

const form = ref({
  name: '',
  goodsName: '',
  goodsDescription: '',
  goodsImage: '',
  originalPrice: null,
  flashPrice: null,
  totalStock: null,
  limitPerUser: 1,
  startTime: '',
  endTime: ''
})

const rules = {
  name: [
    { required: true, message: '请输入活动名称', trigger: 'blur' },
    { min: 2, max: 100, message: '活动名称 2-100 个字符', trigger: 'blur' }
  ],
  goodsName: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 100, message: '商品名称 2-100 个字符', trigger: 'blur' }
  ],
  goodsDescription: [
    { max: 500, message: '商品描述不超过 500 个字符', trigger: 'blur' }
  ],
  originalPrice: [
    { required: true, message: '请输入原价', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '原价须大于 0', trigger: 'blur' }
  ],
  flashPrice: [
    { required: true, message: '请输入秒杀价', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '秒杀价须大于 0', trigger: 'blur' }
  ],
  totalStock: [
    { required: true, message: '请输入总库存', trigger: 'blur' },
    { type: 'number', min: 1, message: '库存须大于 0', trigger: 'blur' }
  ],
  limitPerUser: [
    { required: true, message: '请输入限购数量', trigger: 'blur' },
    { type: 'number', min: 1, message: '限购数量须大于 0', trigger: 'blur' }
  ],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (isEdit.value) return callback()
        if (!value) return callback()
        const now = new Date()
        if (new Date(value).getTime() <= now.getTime() - 60000) {
          callback(new Error('开始时间不能早于当前时间'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (!value || !form.value.startTime) return callback()
        if (new Date(value) <= new Date(form.value.startTime)) {
          callback(new Error('结束时间必须晚于开始时间'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

const statusTagType = computed(() => {
  return ['info', 'warning', 'danger', 'info'][activityStatus.value] || 'info'
})

const statusTagText = computed(() => {
  return ['草稿', '上架中', '进行中', '已结束'][activityStatus.value] || '未知'
})

onMounted(async () => {
  if (isEdit.value) {
    formLoading.value = true
    try {
      const res = await api.getActivityDetail(route.params.id)
      const data = res.data
      activityStatus.value = data.status
      form.value = {
        name: data.name,
        goodsName: data.goodsName,
        goodsDescription: data.goodsDescription || '',
        goodsImage: data.goodsImage || '',
        originalPrice: data.originalPrice,
        flashPrice: data.flashPrice,
        totalStock: data.totalStock,
        limitPerUser: data.limitPerUser,
        startTime: data.startTime,
        endTime: data.endTime
      }
      if (data.goodsImage) {
        fileList.value = [{ name: '商品图片', url: data.goodsImage }]
      }
    } catch (e) {
      ElMessage.error('加载活动信息失败')
      router.push('/admin/activity')
    } finally {
      formLoading.value = false
    }
  }
})

function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.warning('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.warning('图片不能超过 5MB')
    return false
  }
  return true
}

function handleUploadSuccess(response) {
  if (response.code === 200) {
    form.value.goodsImage = response.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

function handleUploadError() {
  ElMessage.error('图片上传失败')
}

function handleRemove() {
  form.value.goodsImage = ''
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 电商校验：秒杀价不能高于原价
  if (form.value.flashPrice >= form.value.originalPrice) {
    ElMessage.warning('秒杀价须低于原价')
    return
  }

  submitting.value = true
  try {
    const payload = {
      ...form.value,
      id: isEdit.value ? Number(route.params.id) : undefined
    }

    if (isEdit.value) {
      await api.updateActivity(payload)
      ElMessage.success('保存成功')
    } else {
      await api.createActivity(payload)
      ElMessage.success('创建成功')
    }

    // 同步到 ES
    try {
      await api.syncSearch()
    } catch {
      // 同步失败不影响主流程
    }

    router.push('/admin/activity')
  } catch (e) {
    // 错误由拦截器统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.form-page { max-width: 800px; margin: 0 auto; }
.page-title { margin-bottom: 24px; }
.upload-wrap { width: 100%; }
.upload-tip { font-size: 12px; color: #999; margin-top: 8px; }
</style>
