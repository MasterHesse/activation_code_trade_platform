<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { useAuthStore } from '@/stores/auth'
import { orderApi, type OrderSummary, type OrderStatus } from '@/api/product'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const orders = ref<OrderSummary[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref<OrderStatus | ''>('')

const statusOptions: { label: string; value: string }[] = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'CREATED' },
  { label: '已支付', value: 'PAID' },
  { label: '发货中', value: 'DELIVERING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '发货失败', value: 'DELIVERY_FAILED' },
  { label: '已取消', value: 'CANCELED' }
]

// 状态映射
const orderStatusMap: Record<string, { label: string; type: string }> = {
  'CREATED': { label: '待支付', type: 'warning' },
  'PAID': { label: '已支付', type: 'success' },
  'DELIVERING': { label: '发货中', type: 'primary' },
  'COMPLETED': { label: '已完成', type: 'success' },
  'DELIVERY_FAILED': { label: '发货失败', type: 'danger' },
  'CANCELED': { label: '已取消', type: 'info' }
}

const paymentStatusMap: Record<string, { label: string; type: string }> = {
  'UNPAID': { label: '未支付', type: 'warning' },
  'PAYING': { label: '支付中', type: 'primary' },
  'PAID': { label: '已支付', type: 'success' },
  'FAILED': { label: '失败', type: 'danger' },
  'CLOSED': { label: '关闭', type: 'info' },
  'REFUNDED': { label: '已退款', type: 'danger' }
}

const loadOrders = async () => {
  if (!authStore.userInfo?.userId) {
    ElMessage.error('请先登录')
    router.push('/login')
    return
  }

  loading.value = true
  try {
    // axios 拦截器返回 response.data，所以 response 结构为 { code, message, data: PageResponse }
    const response = await orderApi.pageUserOrders(authStore.userInfo.userId, {
      page: currentPage.value - 1,
      size: pageSize.value,
      status: statusFilter.value || undefined
    }) as unknown as { data?: { content?: OrderSummary[]; totalElements?: number } }

    // 修正：去掉多余的 .data 层级
    orders.value = response?.data?.content || []
    total.value = response?.data?.totalElements || 0
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '加载订单失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadOrders()
}

const handleStatusFilter = () => {
  currentPage.value = 1
  loadOrders()
}

const handleCancel = async (order: OrderSummary) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消订单「${order.orderNo}」吗？`,
      '取消订单',
      { confirmButtonText: '确定取消', cancelButtonText: '暂不取消', type: 'warning' }
    )

    await orderApi.cancelOrder(order.orderId)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    if (err.response?.data?.message) {
      ElMessage.error(err.response.data.message)
    } else if (err.message !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

const formatDate = (date: string | undefined) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadOrders()
})
</script>

<template>
  <div class="orders-page">
    <div class="page-header">
      <h2 class="page-title">我的订单</h2>
    </div>

    <!-- 筛选 -->
    <el-card class="filter-card">
      <el-select v-model="statusFilter" placeholder="订单状态" clearable @change="handleStatusFilter">
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
    </el-card>

    <!-- 订单列表 -->
    <el-card v-loading="loading" class="orders-card">
      <el-table :data="orders" stripe>
        <el-table-column prop="orderNo" label="订单号" width="200">
          <template #default="{ row }">
            <span class="order-no" @click="router.push(`/orders/${row.orderId}`)">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag :type="orderStatusMap[row.orderStatus]?.type || ''" size="small">
              {{ orderStatusMap[row.orderStatus]?.label || row.orderStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="100">
          <template #default="{ row }">
            <el-tag :type="paymentStatusMap[row.paymentStatus]?.type || ''" size="small">
              {{ paymentStatusMap[row.paymentStatus]?.label || row.paymentStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ row.payAmount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发货方式" width="100">
          <template #default="{ row }">
            {{ row.fulfillmentType === 'CODE_STOCK' ? '卡密' : row.fulfillmentType === 'TOOL_EXECUTION' ? '自动激活' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="发货状态" width="100">
          <template #default="{ row }">
            <span v-if="row.fulfillmentStatus" :class="['fulfillment-status', String(row.fulfillmentStatus).toLowerCase()]">
              {{ row.fulfillmentStatus === 'PENDING' ? '待处理' : row.fulfillmentStatus === 'PROCESSING' ? '处理中' : row.fulfillmentStatus === 'SUCCESS' ? '成功' : row.fulfillmentStatus === 'FAILED' ? '失败' : row.fulfillmentStatus }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/orders/${row.orderId}`)">查看</el-button>
            <el-button
              v-if="row.orderStatus === 'CREATED'"
              size="small"
              type="danger"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="handlePageChange"
        />
      </div>

      <el-empty v-if="!loading && orders.length === 0" description="暂无订单">
        <el-button type="primary" @click="router.push('/products')">去购物</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.orders-page {
  padding: 24px;

  .page-header {
    margin-bottom: 24px;

    .page-title {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: #303133;
    }
  }

  .filter-card {
    margin-bottom: 16px;
  }

  .orders-card {
    .order-no {
      font-family: monospace;
      color: #409eff;
      cursor: pointer;

      &:hover {
        text-decoration: underline;
      }
    }

    .amount {
      font-weight: 600;
      color: #f56c6c;
    }

    .fulfillment-status {
      font-size: 12px;

      &.success {
        color: #67c23a;
      }

      &.failed {
        color: #f56c6c;
      }

      &.processing {
        color: #409eff;
      }
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: center;
    }
  }
}
</style>
