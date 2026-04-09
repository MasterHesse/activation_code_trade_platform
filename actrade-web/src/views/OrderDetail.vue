<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { orderApi, type Order, type PaymentMethod } from '@/api/product'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const paying = ref(false)
const order = ref<Order | null>(null)
const selectedPaymentMethod = ref<PaymentMethod>('MOCK')

const orderId = route.params.id as string

// 订单状态配置
const orderStatusMap: Record<string, { label: string; type: string }> = {
  'CREATED': { label: '待支付', type: 'warning' },
  'PAID': { label: '已支付', type: 'success' },
  'DELIVERING': { label: '发货中', type: 'primary' },
  'COMPLETED': { label: '已完成', type: 'success' },
  'DELIVERY_FAILED': { label: '发货失败', type: 'danger' },
  'CANCELED': { label: '已取消', type: 'info' }
}

// 支付状态配置
const paymentStatusMap: Record<string, { label: string; type: string }> = {
  'UNPAID': { label: '未支付', type: 'warning' },
  'PAYING': { label: '支付中', type: 'primary' },
  'PAID': { label: '已支付', type: 'success' },
  'FAILED': { label: '支付失败', type: 'danger' },
  'CLOSED': { label: '已关闭', type: 'info' },
  'REFUNDED': { label: '已退款', type: 'danger' }
}

// 履约状态配置
const fulfillmentStatusMap: Record<string, { label: string; type: string }> = {
  'PENDING': { label: '待处理', type: 'warning' },
  'PROCESSING': { label: '处理中', type: 'primary' },
  'SUCCESS': { label: '成功', type: 'success' },
  'FAILED': { label: '失败', type: 'danger' }
}

// 支付方式配置
const paymentMethodMap: Record<string, { label: string }> = {
  'MOCK': { label: '模拟支付' },
  'MANUAL': { label: '手动确认' },
  'ALIPAY': { label: '支付宝' }
}

// 发货方式配置
const deliveryModeMap: Record<string, { label: string }> = {
  'CODE_STOCK': { label: '卡密发货' },
  'TOOL_EXECUTION': { label: '自动激活' }
}

// 计算属性
const currentOrderStatus = computed(() => {
  return order.value ? orderStatusMap[order.value.orderStatus] || { label: order.value.orderStatus, type: '' } : null
})

const currentPaymentStatus = computed(() => {
  return order.value ? paymentStatusMap[order.value.paymentStatus] || { label: order.value.paymentStatus, type: '' } : null
})

const currentFulfillmentStatus = computed(() => {
  return order.value?.fulfillmentStatus
    ? fulfillmentStatusMap[order.value.fulfillmentStatus] || { label: order.value.fulfillmentStatus, type: '' }
    : null
})

// 步骤索引
const stepIndex = computed(() => {
  if (!order.value) return 0
  const statusOrder = ['CREATED', 'PAID', 'DELIVERING', 'COMPLETED']
  const idx = statusOrder.indexOf(order.value.orderStatus)
  return idx >= 0 ? idx : 0
})

// 加载订单详情
const loadOrder = async () => {
  loading.value = true
  try {
    // axios 拦截器返回 response.data，结构为 { code, message, data: Order }
    const response = await orderApi.getById(orderId) as unknown as { code?: number; message?: string; data?: Order }
    if (response?.code === 0 && response?.data) {
      order.value = response.data
    } else {
      ElMessage.error('订单不存在')
      router.push('/orders')
    }
  } catch (e: unknown) {
    const err = e as { response?: { status?: number } }
    if (err.response?.status === 404) {
      ElMessage.error('订单不存在')
    } else {
      ElMessage.error('加载订单详情失败')
    }
    router.push('/orders')
  } finally {
    loading.value = false
  }
}

// 发起支付
const handlePay = async () => {
  if (!order.value) return

  try {
    await ElMessageBox.confirm(
      `确定使用「${paymentMethodMap[selectedPaymentMethod.value]?.label}」支付该订单？`,
      '确认支付',
      { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'info' }
    )

    paying.value = true
    // axios 拦截器返回 response.data，结构为 { code, message, data: PaymentInitiateResponse }
    const response = await orderApi.initiatePayment(order.value.orderId, selectedPaymentMethod.value) as unknown as {
      code?: number
      message?: string
      data?: {
        paid?: boolean
        paymentMethod?: string
        orderId?: string
        orderNo?: string
        channelData?: Record<string, string>
      }
    }

    if (response?.code !== 0 || !response?.data) {
      ElMessage.error(response?.message || '支付发起失败')
      return
    }

    const { data } = response

    if (data.paid) {
      ElMessage.success('支付成功！')
      loadOrder()
    } else if (data.paymentMethod === 'ALIPAY' && data.channelData?.formHtml) {
      // 支付宝支付：使用 sessionStorage 存储 formHtml 避免 URL 长度限制
      const paymentData = {
        orderId: data.orderId,
        orderNo: data.orderNo,
        formHtml: data.channelData.formHtml
      }
      sessionStorage.setItem('pending_payment', JSON.stringify(paymentData))

      ElMessage.info('正在跳转到支付宝支付页面...')
      // 在新标签页打开支付网关页面，保持当前页面登录状态
      const gatewayUrl = router.resolve({ name: 'PaymentGateway' }).href
      window.open(gatewayUrl, '_blank')
      // 刷新订单状态查看支付进度
      setTimeout(() => loadOrder(), 3000)
    } else if (data.channelData?.qrCode) {
      // 二维码支付
      ElMessage.info('请使用扫码支付')
      loadOrder()
    } else {
      ElMessage.success('支付请求已提交，请等待确认')
      loadOrder()
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    if (err.response?.data?.message) {
      ElMessage.error(err.response.data.message)
    } else if (err.message !== 'cancel') {
      ElMessage.error('支付失败')
    }
  } finally {
    paying.value = false
  }
}

// 取消订单
const handleCancel = async () => {
  if (!order.value) return

  try {
    await ElMessageBox.confirm('确定要取消该订单吗？取消后不可恢复。', '取消订单', {
      confirmButtonText: '确定取消',
      cancelButtonText: '暂不取消',
      type: 'warning'
    })

    await orderApi.cancelOrder(order.value.orderId)
    ElMessage.success('订单已取消')
    loadOrder()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    if (err.response?.data?.message) {
      ElMessage.error(err.response.data.message)
    } else if (err.message !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

// 确认收货
const handleConfirmReceipt = async () => {
  if (!order.value) return

  try {
    await ElMessageBox.confirm('确定已收到商品且没有问题？', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '暂不确认',
      type: 'info'
    })

    await orderApi.confirmReceipt(order.value.orderId)
    ElMessage.success('已确认收货，订单完成！')
    loadOrder()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    if (err.response?.data?.message) {
      ElMessage.error(err.response.data.message)
    } else if (err.message !== 'cancel') {
      ElMessage.error('确认收货失败')
    }
  }
}

// 格式化日期
const formatDate = (date: string | undefined) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

// 复制文本
const copyText = (text: string, label = '内容') => {
  navigator.clipboard.writeText(text)
  ElMessage.success(`${label}已复制`)
}

onMounted(() => {
  loadOrder()
})
</script>

<template>
  <div class="order-detail-page">
    <div v-loading="loading" class="page-content">
      <template v-if="order">
        <!-- 订单状态卡片 -->
        <el-card class="status-card">
          <div class="order-status-header">
            <div class="status-info">
              <span class="status-label">订单状态</span>
              <el-tag :type="currentOrderStatus?.type || ''" size="large" effect="dark">
                {{ currentOrderStatus?.label || order.orderStatus }}
              </el-tag>
            </div>
            <div class="order-no">
              订单号: <span class="no" @click="copyText(order.orderNo, '订单号')">{{ order.orderNo }}</span>
            </div>
          </div>

          <el-steps v-if="order.orderStatus !== 'CANCELED'" :active="stepIndex" finish-status="success" class="order-steps">
            <el-step title="创建订单" :description="formatDate(order.createdAt)" />
            <el-step title="支付成功" :description="order.paidAt ? formatDate(order.paidAt) : '待支付'" />
            <el-step title="发货" :description="order.deliveredAt ? formatDate(order.deliveredAt) : '待发货'" />
            <el-step title="完成" :description="order.orderStatus === 'COMPLETED' ? formatDate(order.updatedAt) : ''" />
          </el-steps>
        </el-card>

        <el-row :gutter="16">
          <!-- 订单信息 -->
          <el-col :xs="24" :lg="16">
            <el-card class="info-card">
              <template #header>
                <span>订单信息</span>
              </template>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="订单金额">
                  <span class="price-value">¥{{ order.totalAmount.toFixed(2) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="实付金额">
                  <span class="price-value highlight">¥{{ order.payAmount.toFixed(2) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="支付方式">
                  {{ order.paymentMethod ? paymentMethodMap[order.paymentMethod]?.label || order.paymentMethod : '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="支付状态">
                  <el-tag :type="currentPaymentStatus?.type || ''" size="small">
                    {{ currentPaymentStatus?.label || order.paymentStatus }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="发货方式">
                  {{ order.items?.[0]?.deliveryMode ? deliveryModeMap[order.items[0].deliveryMode]?.label || order.items[0].deliveryMode : '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="发货状态">
                  <el-tag v-if="currentFulfillmentStatus" :type="currentFulfillmentStatus.type" size="small">
                    {{ currentFulfillmentStatus.label }}
                  </el-tag>
                  <span v-else>-</span>
                </el-descriptions-item>
                <el-descriptions-item label="结算状态" :span="2">
                  <el-tag :type="order.settlementStatus === 'SETTLED' ? 'success' : 'info'" size="small">
                    {{ order.settlementStatus === 'SETTLED' ? '已结算' : '待结算' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item v-if="order.remark" label="备注" :span="2">
                  {{ order.remark }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>

            <!-- 商品列表 -->
            <el-card class="items-card">
              <template #header>
                <span>商品明细</span>
              </template>
              <el-table :data="order.items" stripe>
                <el-table-column prop="productName" label="商品名称" min-width="150" />
                <el-table-column label="发货方式" width="100">
                  <template #default="{ row }">
                    {{ deliveryModeMap[row.deliveryMode]?.label || row.deliveryMode }}
                  </template>
                </el-table-column>
                <el-table-column label="单价" width="100">
                  <template #default="{ row }">
                    ¥{{ row.unitPrice.toFixed(2) }}
                  </template>
                </el-table-column>
                <el-table-column prop="quantity" label="数量" width="80" align="center" />
                <el-table-column label="小计" width="100">
                  <template #default="{ row }">
                    <span class="price-value">¥{{ row.subtotalAmount.toFixed(2) }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>

          <!-- 时间线 & 操作 -->
          <el-col :xs="24" :lg="8">
            <!-- 时间线 -->
            <el-card class="timeline-card">
              <template #header>
                <span>订单时间</span>
              </template>
              <el-timeline>
                <el-timeline-item :timestamp="formatDate(order.createdAt)" placement="top">
                  订单创建
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.payDeadlineAt"
                  :timestamp="formatDate(order.payDeadlineAt)"
                  placement="top"
                  type="warning"
                >
                  支付截止
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.paidAt"
                  :timestamp="formatDate(order.paidAt)"
                  placement="top"
                  type="success"
                >
                  支付成功
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.deliveredAt"
                  :timestamp="formatDate(order.deliveredAt)"
                  placement="top"
                  type="primary"
                >
                  商品发货
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.confirmDeadlineAt"
                  :timestamp="formatDate(order.confirmDeadlineAt)"
                  placement="top"
                  type="info"
                >
                  确认截止
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.orderStatus === 'COMPLETED'"
                  :timestamp="formatDate(order.updatedAt)"
                  placement="top"
                  type="success"
                >
                  订单完成
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.orderStatus === 'CANCELED'"
                  :timestamp="formatDate(order.closedAt || order.updatedAt)"
                  placement="top"
                  type="info"
                >
                  订单取消
                </el-timeline-item>
              </el-timeline>
            </el-card>

            <!-- 操作面板 -->
            <el-card v-if="order.orderStatus === 'CREATED'" class="action-card">
              <template #header>
                <span>待支付</span>
              </template>
              <div class="payment-selection">
                <p class="tip">请选择支付方式：</p>
                <el-radio-group v-model="selectedPaymentMethod">
                  <el-radio value="MOCK">模拟支付（测试用）</el-radio>
                  <el-radio value="MANUAL">手动确认</el-radio>
                  <el-radio value="ALIPAY">支付宝</el-radio>
                </el-radio-group>
              </div>
              <div class="action-buttons">
                <el-button type="primary" size="large" :loading="paying" style="width: 100%" @click="handlePay">
                  确认支付 ¥{{ order.payAmount.toFixed(2) }}
                </el-button>
                <el-button size="large" style="width: 100%; margin-top: 8px" @click="handleCancel">
                  取消订单
                </el-button>
              </div>
            </el-card>

            <!-- 支付中状态面板 -->
            <el-card v-else-if="order.paymentStatus === 'PAYING'" class="action-card paying">
              <template #header>
                <span>支付中</span>
              </template>
              <div class="paying-content">
                <el-icon :size="48" color="#409eff" class="is-loading"><Loading /></el-icon>
                <p class="paying-tip">您的订单正在等待支付确认</p>
                <p class="paying-sub-tip">请在支付宝完成支付后，点击下方按钮刷新订单状态</p>
              </div>
              <div class="action-buttons">
                <el-button type="primary" size="large" style="width: 100%" @click="loadOrder">
                  刷新订单状态
                </el-button>
                <el-button size="large" style="width: 100%; margin-top: 8px" @click="handleCancel">
                  取消订单
                </el-button>
              </div>
            </el-card>

            <el-card v-else-if="order.orderStatus === 'DELIVERING' || order.fulfillmentStatus === 'SUCCESS'" class="action-card">
              <template #header>
                <span>待确认</span>
              </template>
              <p class="tip">如果已收到激活码/激活成功，请点击确认收货完成订单。</p>
              <el-button type="success" size="large" style="width: 100%" @click="handleConfirmReceipt">
                确认收货
              </el-button>
            </el-card>

            <el-card v-else-if="order.orderStatus === 'COMPLETED'" class="action-card success">
              <div class="success-info">
                <el-icon :size="48" color="#67c23a"><CircleCheck /></el-icon>
                <p>订单已完成</p>
                <span class="sub-tip">感谢您的购买，欢迎再次光临</span>
              </div>
            </el-card>

            <el-card v-else-if="order.orderStatus === 'CANCELED'" class="action-card">
              <div class="cancel-info">
                <el-icon :size="48" color="#909399">< CircleCheck /></el-icon>
                <p>订单已取消</p>
              </div>
            </el-card>

            <!-- 返回按钮 -->
            <div class="back-button">
              <el-button size="large" @click="router.push('/orders')">
                返回订单列表
              </el-button>
            </div>
          </el-col>
        </el-row>
      </template>

      <el-empty v-else description="订单加载中..." />
    </div>
  </div>
</template>

<style scoped lang="scss">
.order-detail-page {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: #f5f7fa;

  .page-content {
    max-width: 1200px;
    margin: 0 auto;
  }

  .status-card {
    margin-bottom: 16px;

    .order-status-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;

      .status-info {
        display: flex;
        align-items: center;
        gap: 12px;

        .status-label {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
        }
      }

      .order-no {
        color: #606266;

        .no {
          cursor: pointer;
          color: #409eff;
          font-family: monospace;

          &:hover {
            text-decoration: underline;
          }
        }
      }
    }

    .order-steps {
      margin-top: 24px;
    }
  }

  .info-card {
    margin-bottom: 16px;

    .price-value {
      font-weight: 600;
      color: #303133;
    }

    .price-value.highlight {
      color: #f56c6c;
      font-size: 16px;
    }
  }

  .items-card {
    margin-bottom: 16px;
  }

  .timeline-card {
    margin-bottom: 16px;
  }

  .action-card {
    margin-bottom: 16px;

    .tip {
      color: #909399;
      font-size: 14px;
      margin: 0 0 16px;
    }

    .payment-selection {
      margin-bottom: 16px;

      :deep(.el-radio-group) {
        display: flex;
        flex-direction: column;
        gap: 12px;
      }
    }

    .success-info {
      text-align: center;
      padding: 20px 0;

      p {
        margin: 16px 0 8px;
        font-size: 18px;
        font-weight: 600;
        color: #67c23a;
      }

      .sub-tip {
        color: #909399;
        font-size: 14px;
      }
    }

    .cancel-info {
      text-align: center;
      padding: 20px 0;

      p {
        margin: 16px 0 8px;
        font-size: 18px;
        font-weight: 600;
        color: #909399;
      }
    }

    &.paying {
      border-color: #409eff;

      .paying-content {
        text-align: center;
        padding: 20px 0;

        .paying-tip {
          margin: 16px 0 8px;
          font-size: 16px;
          font-weight: 600;
          color: #303133;
        }

        .paying-sub-tip {
          color: #909399;
          font-size: 14px;
          margin-bottom: 16px;
        }
      }
    }
  }

  .back-button {
    margin-top: 16px;
    text-align: center;
  }
}
</style>
