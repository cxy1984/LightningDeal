<template>
  <div class="manage-page">
    <div class="page-header">
      <h2>📋 活动管理</h2>
      <div class="header-actions">
        <el-button @click="handleSyncSearch" :loading="syncLoading">
          <el-icon><Upload /></el-icon> 同步到 ES
        </el-button>
        <el-button type="primary" @click="$router.push('/admin/activity/create')">
          <el-icon><Plus /></el-icon>创建活动
        </el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" stripe style="width: 100%">
      <el-table-column label="ID" prop="id" width="80" align="center" />
      <el-table-column label="活动名称" prop="name" min-width="180" />
      <el-table-column label="商品名称" prop="goodsName" min-width="150" />
      <el-table-column label="秒杀价" width="100" align="center">
        <template #default="{ row }">
          <span style="color:#e74c3c;font-weight:bold">¥{{ row.flashPrice }}</span>
        </template>
      </el-table-column>
      <el-table-column label="库存" width="100" align="center">
        <template #default="{ row }">
          {{ row.remainStock }}/{{ row.totalStock }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" prop="startTime" width="170" />
      <el-table-column label="结束时间" prop="endTime" width="170" />
      <el-table-column label="操作" width="280" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="$router.push(`/admin/activity/edit/${row.id}`)">
            编辑
          </el-button>
          <template v-if="row.status < 2">
            <el-button
              v-if="row.dbStatus === 0"
              type="success"
              size="small"
              :loading="statusLoading === row.id"
              @click="handleStatus(row.id, 1)"
            >
              上架
            </el-button>
            <el-button
              v-if="row.dbStatus === 1"
              type="warning"
              size="small"
              :loading="statusLoading === row.id"
              @click="handleStatus(row.id, 0)"
            >
              下架
            </el-button>
            <el-button
              type="danger"
              size="small"
              :loading="deleteLoading === row.id"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
          <span v-else style="color:#999;font-size:12px">-</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchList"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'

const list = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)
const statusLoading = ref(null)
const deleteLoading = ref(null)
const syncLoading = ref(false)

onMounted(() => fetchList())

async function fetchList() {
  loading.value = true
  try {
    const res = await api.getActivityList({ page: page.value, size: size.value })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function statusText(status) {
  return ['草稿', '上架中', '进行中', '已结束'][status] || '未知'
}

function statusType(status) {
  return ['info', 'warning', 'danger', 'info'][status] || 'info'
}

async function handleStatus(id, status) {
  statusLoading.value = id
  try {
    await api.updateActivityStatus(id, status)
    ElMessage.success(status === 1 ? '已上架' : '已下架')
    await fetchList()
  } finally {
    statusLoading.value = null
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除活动「${row.name}」吗？`, '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    deleteLoading.value = row.id
    await api.deleteActivity(row.id)
    ElMessage.success('删除成功')
    await fetchList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  } finally {
    deleteLoading.value = null
  }
}

async function handleSyncSearch() {
  syncLoading.value = true
  try {
    const res = await api.syncSearch()
    ElMessage.success(res.data || '同步成功')
  } catch {
    ElMessage.error('同步失败')
  } finally {
    syncLoading.value = false
  }
}
</script>

<style scoped>
.manage-page { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header-actions { display: flex; gap: 12px; }
.pagination { text-align: center; margin-top: 24px; }
</style>
