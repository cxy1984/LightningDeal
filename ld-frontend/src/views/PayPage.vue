<template>
  <div class="pay-page">
    <el-card class="pay-card">
      <template #header>
        <div class="pay-header">
          <span>💳 确认支付</span>
          <el-tag :type="order?.status === 0 ? 'warning' : 'success'" size="large">
            {{ order?.statusText || '未知' }}
          </el-tag>
        </div>
      </template>

      <div v-if="loading" class="loading-wrapper">
        <el-skeleton :rows="5" animated />
      </div>

      <template v-else-if="order">
        <!-- 商品信息 -->
        <div class="goods-section">
          <el-image v-if="order.goodsImage" :src="order.goodsImage" class="goods-image" fit="cover" />
          <div class="goods-detail">
            <h3>{{ order.goodsName }}</h3>
            <p class="order-no">订单号：{{ order.orderNo }}</p>
          </div>
        </div>

        <el-divider />

        <!-- 金额 -->
        <div class="amount-section">
          <div class="amount-row">
            <span>商品金额</span>
            <span>¥{{ order.flashPrice }} × {{ order.quantity }}</span>
          </div>
          <div class="amount-row total">
            <span>实付金额</span>
            <span class="total-price">¥{{ order.totalAmount }}</span>
          </div>
        </div>

        <el-divider />

        <!-- 支付方式 -->
        <div class="pay-methods">
          <h4>选择支付方式</h4>
          <div class="method-list">
            <div
              v-for="m in payMethods"
              :key="m.id"
              class="method-item"
              :class="{ active: selectedMethod === m.id }"
              @click="selectedMethod = m.id"
            >
              <span class="method-icon">{{ m.icon }}</span>
              <span class="method-name">{{ m.name }}</span>
              <el-icon v-if="selectedMethod === m.id" class="check-icon"><Check /></el-icon>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 操作按钮 -->
        <div class="action-bar">
          <el-button size="large" @click="handleCancel">取消</el-button>
          <el-button type="primary" size="large" :loading="paying" @click="handlePay" class="pay-btn">
            ✅ 确认支付 ¥{{ order.totalAmount }}
          </el-button>
        </div>
      </template>

      <el-empty v-else description="订单不存在或已支付" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const loading = ref(true)
const paying = ref(false)
const selectedMethod = ref('wechat')

const payMethods = [
  { id: 'wechat', name: '微信支付', icon: '💚' },
  { id: 'alipay', name: '支付宝', icon: '💙' },
  { id: 'card', name: '银行卡支付', icon: '💳' },
  { id: 'balance', name: '余额支付', icon: '💰' }
]

onMounted(async () => {
  try {
    const res = await api.getOrderDetail(route.params.id)
    order.value = res.data
  } catch {
    order.value = null
  } finally {
    loading.value = false
  }
})

async function handlePay() {
  paying.value = true
  try {
    await api.payOrder(order.value.id)
    ElMessage.success('🎉 支付成功！')
    // 支付成功跳转到订单详情
    router.replace(`/order/detail/${order.value.id}`)
  } catch {
    // api 内部已处理错误提示
  } finally {
    paying.value = false
  }
}

function handleCancel() {
  router.back()
}
</script>

<style scoped>
.pay-page {
  max-width: 600px;
  margin: 0 auto;
}
.loading-wrapper { padding: 40px; }

.pay-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.goods-section {
  display: flex;
  gap: 20px;
  align-items: center;
}
.goods-image {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  flex-shrink: 0;
  background: #f5f5f5;
}
.goods-detail { flex: 1; }
.goods-detail h3 { margin: 0 0 8px; font-size: 18px; }
.order-no { color: #999; font-size: 13px; margin: 0; }

.amount-section {  }
.amount-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 14px;
  color: #666;
}
.amount-row.total {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  padding-top: 12px;
  border-top: 1px dashed #eee;
  margin-top: 8px;
}
.total-price { color: #e74c3c; font-size: 24px; }

.pay-methods h4 { margin: 0 0 16px; font-size: 15px; }
.method-list { display: flex; flex-direction: column; gap: 8px; }
.method-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.method-item:hover { border-color: #409eff; }
.method-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}
.method-icon { font-size: 24px; }
.method-name { flex: 1; font-size: 15px; }
.check-icon { color: #409eff; font-size: 18px; }

.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}
.pay-btn { min-width: 200px; }
</style>
