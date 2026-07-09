<template>
  <div class="orders-page">
    <h2 class="page-title">📦 我的订单</h2>

    <el-tabs v-model="statusFilter" @tab-change="fetchOrders">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待支付" name="0" />
      <el-tab-pane label="已支付" name="1" />
      <el-tab-pane label="已取消" name="2" />
    </el-tabs>

    <el-table :data="list" style="width:100%" v-loading="loading" stripe>
      <el-table-column label="订单编号" prop="orderNo" width="200" />
      <el-table-column label="商品" prop="goodsName" min-width="150" />
      <el-table-column label="金额" width="120">
        <template #default="{ row }">
          <span style="color:#e74c3c;font-weight:bold">¥{{ row.totalAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="数量" prop="quantity" width="80" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="180" />
      <el-table-column label="操作" width="280" align="center">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="$router.push('/order/detail/' + row.id)">详情</el-button>
                    <el-button v-if="row.status === 0" type="success" size="small" @click="$router.push('/order/pay/' + row.id)">支付</el-button>
          <el-button v-if="row.status === 0" type="info" size="small" @click="handleCancel(row.id)">取消</el-button>
          <el-button v-if="row.status === 1" type="danger" size="small" @click="handleRefund(row)">退款</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchOrders"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const statusFilter = ref('')

onMounted(() => fetchOrders())

async function fetchOrders() {
  loading.value = true
  try {
    const res = await api.getOrders({
      page: page.value,
      size: size.value,
      status: statusFilter.value || undefined
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function statusTagType(status) {
  return ['warning', 'success', 'info', 'danger'][status] || 'info'
}

async function handleCancel(orderId) {
  await ElMessageBox.confirm('确定取消该订单吗？', '提示')
  await api.cancelOrder(orderId)
  ElMessage.success('已取消')
  fetchOrders()
}

async function handleRefund(row) {
  await ElMessageBox.confirm(
    `确定要对「${row.goodsName}」申请退款吗？\n退款金额：¥${row.totalAmount}`,
    '退款确认',
    { confirmButtonText: '申请退款', cancelButtonText: '取消', type: 'warning' }
  )
  await api.refundOrder(row.id)
  ElMessage.success('退款成功')
  fetchOrders()
}
</script>

<style scoped>
.orders-page { max-width: 1200px; margin: 0 auto; }
.page-title { margin-bottom: 20px; }
.pagination { text-align: center; margin-top: 24px; }
</style>
