<template>
  <div class="form-page">
    <h2 class="page-title">{{ isEdit ? '编辑活动' : '创建活动' }}</h2>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 700px">
      <el-form-item label="活动名称" prop="name">
        <el-input v-model="form.name" placeholder="例：iPhone 16 限时秒杀" />
      </el-form-item>

      <el-form-item label="商品名称" prop="goodsName">
        <el-input v-model="form.goodsName" placeholder="例：iPhone 16 256GB" />
      </el-form-item>

      <el-form-item label="商品描述" prop="goodsDescription">
        <el-input v-model="form.goodsDescription" type="textarea" :rows="3" placeholder="商品描述信息" />
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
            :on-exceed="() => $message.warning('只能上传一张图片')"
            :on-remove="handleRemove"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议尺寸 800x800，支持 JPG/PNG，最大 5MB</div>
        </div>
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="原价" prop="originalPrice">
            <el-input v-model.number="form.originalPrice" type="number" min="0" step="0.01" placeholder="0.00">
              <template #prefix>¥</template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="秒杀价" prop="flashPrice">
            <el-input v-model.number="form.flashPrice" type="number" min="0" step="0.01" placeholder="0.00">
              <template #prefix>¥</template>
            </el-input>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="总库存" prop="totalStock">
            <el-input v-model.number="form.totalStock" type="number" min="1" placeholder="100" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="限购数量" prop="limitPerUser">
            <el-input v-model.number="form.limitPerUser" type="number" min="1" placeholder="1" />
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
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '创建活动' }}
        </el-button>
        <el-button size="large" @click="$router.push('/admin/activity')">取消</el-button>
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
const fileList = ref([])

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
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  goodsName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  originalPrice: [{ required: true, message: '请输入原价', trigger: 'blur' }],
  flashPrice: [{ required: true, message: '请输入秒杀价', trigger: 'blur' }],
  totalStock: [{ required: true, message: '请输入总库存', trigger: 'blur' }],
  limitPerUser: [{ required: true, message: '请输入限购数量', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await api.getActivityDetail(route.params.id)
      const data = res.data
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
