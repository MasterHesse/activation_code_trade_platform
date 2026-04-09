<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const loading = ref(true)
const orderNo = ref('')
const orderId = ref('')
const errorMessage = ref('')

// 从 sessionStorage 获取支付数据
interface PaymentData {
  orderId: string
  orderNo: string
  formHtml: string
}

function getPaymentData(): PaymentData | null {
  try {
    const stored = sessionStorage.getItem('pending_payment')
    return stored ? JSON.parse(stored) : null
  } catch {
    return null
  }
}

onMounted(() => {
  const paymentData = getPaymentData()

  if (!paymentData || !paymentData.formHtml) {
    errorMessage.value = '未获取到支付表单，可能已超时，请返回订单页面重新发起支付'
    loading.value = false
    return
  }

  orderNo.value = paymentData.orderNo
  orderId.value = paymentData.orderId

  try {
    // 将 formHtml 渲染到页面中
    const container = document.getElementById('payment-form-container')
    if (container) {
      container.innerHTML = paymentData.formHtml

      // 等待 DOM 更新后自动提交表单
      setTimeout(() => {
        const form = document.querySelector('form') as HTMLFormElement
        if (form) {
          // 清除 sessionStorage 中的支付数据（已传递给支付页面）
          // sessionStorage.removeItem('pending_payment')
          form.submit()
        } else {
          errorMessage.value = '支付表单格式错误，请返回订单页面重试'
          loading.value = false
        }
      }, 100)
    }
  } catch (e) {
    errorMessage.value = '支付表单解析失败，请返回订单页面重试'
    loading.value = false
  }
})

const goBack = () => {
  if (orderId.value) {
    router.push(`/orders/${orderId.value}`)
  } else {
    router.push('/orders')
  }
}
</script>

<template>
  <div class="payment-gateway-page">
    <div class="payment-container">
      <el-card v-if="loading && !errorMessage" class="loading-card">
        <div class="loading-content">
          <el-icon class="is-loading" :size="48" color="#409eff"><Loading /></el-icon>
          <h2>正在跳转到支付宝支付页面...</h2>
          <p class="tip">请在新打开的页面完成支付，支付完成后页面将自动跳转</p>
          <p v-if="orderNo" class="order-no">订单号: {{ orderNo }}</p>
        </div>
      </el-card>

      <el-card v-if="errorMessage" class="error-card">
        <div class="error-content">
          <el-icon :size="48" color="#f56c6c"><CircleCloseFilled /></el-icon>
          <h2>支付页面加载失败</h2>
          <p class="error-msg">{{ errorMessage }}</p>
          <el-button type="primary" size="large" @click="goBack">
            返回订单页面
          </el-button>
        </div>
      </el-card>

      <!-- 隐藏的表单容器，支付表单将在这里渲染 -->
      <div id="payment-form-container" style="display: none;"></div>

      <!-- 备用：直接显示支付链接按钮 -->
      <div v-if="!loading && !errorMessage" class="fallback-actions">
        <p class="fallback-tip">如果支付页面没有自动跳转，请点击下方按钮</p>
        <el-button type="primary" size="large" @click="goBack">
          返回订单页面
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.payment-gateway-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;

  .payment-container {
    width: 100%;
    max-width: 480px;
  }

  .loading-card,
  .error-card {
    text-align: center;

    .loading-content,
    .error-content {
      padding: 40px 20px;

      h2 {
        margin: 24px 0 16px;
        color: #303133;
      }

      .tip {
        color: #909399;
        margin-bottom: 8px;
      }

      .order-no {
        font-family: monospace;
        color: #606266;
        font-size: 14px;
      }

      .error-msg {
        color: #f56c6c;
        margin: 16px 0 24px;
      }
    }
  }

  .fallback-actions {
    margin-top: 24px;
    text-align: center;

    .fallback-tip {
      color: rgba(255, 255, 255, 0.9);
      margin-bottom: 16px;
    }
  }
}
</style>
