<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { useAuthStore } from '@/stores/auth'
import { merchantApi, type Merchant } from '@/api/merchant'
import { orderApi, type OrderSummary, type OrderStatus } from '@/api/product'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const merchant = ref<Merchant | null>(null)
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

const loadMerchant = async () => {
  if (!authStore.userInfo?.userId) {
    ElMessage.error('请先登录')
    router.push('/login')
    return
  }

  try {
    // 响应结构为 { code: 200, message: "success", data: Merchant } 或 { code: 404, ... }
    const response = await merchantApi.getByUserId(authStore.userInfo.userId) as unknown as { code?: number; data?: Merchant }
    if (response?.code === 200 && response.data) {
      merchant.value = response.data
      if (merchant.value?.auditStatus !== 'APPROVED') {
        ElMessage.warning('您的商家资质尚未审核通过')
        router.push('/merchant/apply')
      }
    } else {
      ElMessage.warning('您还未申请成为商家')
      router.push('/merchant/apply')
    }
  } catch {
    ElMessage.warning('您还未申请成为商家')
    router.push('/merchant/apply')
  }
}

const loadOrders = async () => {
  if (!merchant.value?.merchantId) return

  loading.value = true
  try {
    // axios 拦截器返回 response.data，所以 response 结构为 { code, message, data: PageResponse }
    const response = await orderApi.pageMerchantOrders(merchant.value.merchantId, {
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

const formatDate = (date: string | undefined) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadOrders()
}

const handleStatusFilter = () => {
  currentPage.value = 1
  loadOrders()
}

// 手动发货
const handleDeliver = async (order: OrderSummary) => {
  try {
    await ElMessageBox.confirm(
      `确定要为订单「${order.orderNo}」发货吗？`,
      '确认发货',
      { confirmButtonText: '确认发货', cancelButtonText: '取消', type: 'info' }
    )

    await orderApi.deliverOrder(order.orderId)
    ElMessage.success('发货成功')
    loadOrders()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    if (err.response?.data?.message) {
      ElMessage.error(err.response.data.message)
    } else if (err.message !== 'cancel') {
      ElMessage.error('发货失败')
    }
  }
}

// 标记发货失败
const handleDeliveryFailed = async (order: OrderSummary) => {
  try {
    await ElMessageBox.confirm(
      `确定订单「${order.orderNo}」发货失败吗？`,
      '标记失败',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )

    await orderApi.markDeliveryFailed(order.orderId)
    ElMessage.success('已标记为发货失败')
    loadOrders()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    if (err.response?.data?.message) {
      ElMessage.error(err.response.data.message)
    } else if (err.message !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(async () => {
  await loadMerchant()
  loadOrders()
})
</script>

<template>
  <div class="merchant-orders-page">
    <div class="page-header">
      <h2 class="page-title">订单管理</h2>
      <div class="header-info">
        <el-tag v-if="merchant" type="success">{{ merchant.merchantName }}</el-tag>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-select
        v-model="statusFilter"
        placeholder="订单状态"
        clearable
        @change="handleStatusFilter"
      >
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
            <span class="order-no">{{ row.orderNo }}</span>
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
        <el-table-column label="商家收入" width="120">
          <template #default="{ row }">
            <span class="amount income">¥{{ (row.sellerIncomeAmount || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="结算状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.settlementStatus === 'SETTLED' ? 'success' : 'info'" size="small">
              {{ row.settlementStatus === 'SETTLED' ? '已结算' : '待结算' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/orders/${row.orderId}`)">查看</el-button>
            <el-button
              v-if="row.orderStatus === 'PAID'"
              type="primary"
              size="small"
              @click="handleDeliver(row)"
            >
              发货
            </el-button>
            <el-button
              v-if="row.orderStatus === 'DELIVERING'"
              type="warning"
              size="small"
              @click="handleDeliveryFailed(row)"
            >
              失败
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

      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.merchant-orders-page {
  padding: 24px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
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

    .amount.income {
      color: #67c23a;
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: center;
    }
  }
}
</style>
