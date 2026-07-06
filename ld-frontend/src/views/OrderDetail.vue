<template>
  <div class="detail-page">
    <div class="back-link">
      <el-button text @click="$router.back()">← 返回订单列表</el-button>
    </div>

    <div v-if="loading" class="loading-wrapper">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else-if="order">
      <div class="detail-card">
        <div class="header">
          <h2>订单详情</h2>
          <el-tag :type="tagType" size="large">{{ order.statusText }}</el-tag>
        </div>

        <el-divider />

        <div class="goods-section">
          <el-image
            v-if="order.goodsImage"
            :src="order.goodsImage"
            class="goods-image"
            fit="cover"
          />
          <div class="goods-info">
            <h3>{{ order.goodsName }}</h3>
            <div class="price-row">
              <span class="flash-price">¥{{ order.flashPrice }}</span>
              <span class="quantity">× {{ order.quantity }}</span>
            </div>
            <div class="total-amount">
              实付：<span class="amount">¥{{ order.totalAmount }}</span>
            </div>
          </div>
        </div>

        <el-divider />

        <div class="info-section">
          <div class="info-row">
            <span class="label">订单编号</span>
            <span class="value">{{ order.orderNo }}</span>
          </div>
          <div class="info-row">
            <span class="label">创建时间</span>
            <span class="value">{{ order.createTime }}</span>
          </div>
          <div class="info-row" v-if="order.status === 1">
            <span class="label">支付时间</span>
            <span class="value">{{ order.payTime || '-' }}</span>
          </div>
        </div>

        <el-divider />

        <div class="actions">
          <el-button v-if="order.status === 0" type="success" size="large" @click="handlePay">
            立即支付 ¥{{ order.totalAmount }}
          </el-button>
          <el-button v-if="order.status === 0" type="info" @click="handleCancel">
            取消订单
          </el-button>
          <el-button v-if="order.status === 1" type="danger" @click="handleRefund">
            申请退款
          </el-button>
        </div>
      </div>
    </template>

    <el-empty v-else description="订单不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await api.getOrderDetail(route.params.id)
    order.value = res.data
  } catch (e) {
    order.value = null
  } finally {
    loading.value = false
  }
})

const tagType = () => {
  const map = ['warning', 'success', 'info', 'danger']
  return map[order.value?.status] || 'info'
}

async function handlePay() {
  await api.payOrder(order.value.id)
  ElMessage.success('支付成功')
  // 刷新
  const res = await api.getOrderDetail(route.params.id)
  order.value = res.data
}

async function handleCancel() {
  await ElMessageBox.confirm('确定取消该订单吗？', '提示')
  await api.cancelOrder(order.value.id)
  ElMessage.success('已取消')
  const res = await api.getOrderDetail(route.params.id)
  order.value = res.data
}

async function handleRefund() {
  await ElMessageBox.confirm('确定要退款吗？退款后将恢复您的购买资格。', '退款确认', {
    confirmButtonText: '申请退款',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await api.refundOrder(order.value.id)
  ElMessage.success('退款成功')
  const res = await api.getOrderDetail(route.params.id)
  order.value = res.data
}
</script>

<style scoped>
.detail-page { max-width: 800px; margin: 0 auto; }
.back-link { margin-bottom: 16px; }
.loading-wrapper { padding: 40px; }
.detail-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header h2 { margin: 0; }
.goods-section {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}
.goods-image {
  width: 160px;
  height: 160px;
  border-radius: 8px;
  flex-shrink: 0;
  background: #f5f5f5;
}
.goods-info { flex: 1; }
.goods-info h3 { margin: 0 0 12px; font-size: 18px; }
.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}
.flash-price { color: #e74c3c; font-size: 22px; font-weight: bold; }
.quantity { color: #999; font-size: 14px; }
.total-amount { font-size: 16px; }
.amount { color: #e74c3c; font-weight: bold; font-size: 20px; }
.info-section {  }
.info-row {
  display: flex;
  padding: 8px 0;
}
.label { width: 100px; color: #999; flex-shrink: 0; }
.value { color: #333; }
.actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 8px 0;
}
</style>
