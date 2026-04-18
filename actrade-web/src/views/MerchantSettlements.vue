<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import { merchantApi, type Merchant } from '@/api/merchant'
import { settlementApi, type SellerSettlement, type SettlementStatus } from '@/api/product'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const merchant = ref<Merchant | null>(null)
const pageLoading = ref(true)

// 确保用户已登录并获取最新用户信息
const ensureAuthenticated = async (): Promise<string | null> => {
  try {
    const authResponse = await authApi.getCurrentUser() as { userId?: string; username?: string; role?: string }
    if (authResponse?.userId) {
      authStore.setUserInfo({
        userId: authResponse.userId,
        username: authResponse.username || authResponse.userId,
        role: authResponse.role || 'ROLE_USER'
      })
      return authResponse.userId
    }
  } catch {
    ElMessage.error('获取用户信息失败，请重新登录')
    router.push('/login')
  }
  return null
}
const settlements = ref<SellerSettlement[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(Math.min(10, 100)) // 确保不超过后端限制 100
const statusFilter = ref<SettlementStatus | undefined>(undefined)

const statusOptions: { label: string; value: SettlementStatus | undefined }[] = [
  { label: '全部', value: undefined },
  { label: '待结算', value: 'UNSETTLED' },
  { label: '结算中', value: 'PENDING' },
  { label: '已结算', value: 'SETTLED' }
]

// 状态映射
const settlementStatusMap: Record<string, { label: string; type: string }> = {
  'UNSETTLED': { label: '待结算', type: 'warning' },
  'PENDING': { label: '结算中', type: 'primary' },
  'SETTLED': { label: '已结算', type: 'success' }
}

const loadMerchant = async () => {
  const userId = await ensureAuthenticated()
  if (!userId) {
    pageLoading.value = false
    return
  }

  try {
    // 后端返回结构为 { code: 200, message: "success", data: Merchant } 或 { code: 404, ... }
    const response = await merchantApi.getByUserId(userId) as unknown as { code?: number; data?: Merchant }
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
  } finally {
    pageLoading.value = false
  }
}

const loadSettlements = async () => {
  if (!merchant.value?.merchantId) return

  loading.value = true
  try {
    // axios 拦截器返回 response.data，所以 response 结构为 { code, message, data: PageResponse }
    const response = await settlementApi.pageMerchantSettlements(merchant.value.merchantId, {
      page: currentPage.value - 1,
      size: pageSize.value,
      status: statusFilter.value || undefined
    }) as unknown as { data?: { content?: SellerSettlement[]; totalElements?: number } }

    // 修正：去掉多余的 .data 层级
    settlements.value = response?.data?.content || []
    total.value = response?.data?.totalElements || 0
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '加载结算记录失败')
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
  loadSettlements()
}

const handleStatusFilter = () => {
  currentPage.value = 1
  loadSettlements()
}

// 统计信息
const statistics = ref({
  totalAmount: 0,
  unsettledAmount: 0,
  settledAmount: 0
})

const loadStatistics = async () => {
  if (!merchant.value?.merchantId) return

  try {
    // 使用后端允许的最大 size 值
    // axios 拦截器返回 response.data，所以 response 结构为 { code, message, data: PageResponse }
    const response = await settlementApi.pageMerchantSettlements(merchant.value.merchantId, { size: 100 }) as unknown as { data?: { content?: SellerSettlement[] } }
    // 修正：去掉多余的 .data 层级
    const data = response?.data?.content || []

    statistics.value = {
      totalAmount: data.reduce((sum: number, s: SellerSettlement) => sum + (s.settlementAmount || 0), 0),
      unsettledAmount: data.filter((s: SellerSettlement) => s.settlementStatus !== 'SETTLED').reduce((sum: number, s: SellerSettlement) => sum + (s.settlementAmount || 0), 0),
      settledAmount: data.filter((s: SellerSettlement) => s.settlementStatus === 'SETTLED').reduce((sum: number, s: SellerSettlement) => sum + (s.settlementAmount || 0), 0)
    }
  } catch {
    // ignore
  }
}

onMounted(async () => {
  await loadMerchant()
  loadSettlements()
  loadStatistics()
})
</script>

<template>
  <div class="merchant-settlements-page">
    <div class="page-header">
      <h2 class="page-title">结算查询</h2>
      <div class="header-info">
        <el-tag v-if="merchant" type="success">{{ merchant.merchantName }}</el-tag>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="statistics-row">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-label">累计收入</div>
            <div class="stat-value">¥{{ statistics.totalAmount.toFixed(2) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-label">待结算</div>
            <div class="stat-value warning">¥{{ statistics.unsettledAmount.toFixed(2) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-label">已结算</div>
            <div class="stat-value success">¥{{ statistics.settledAmount.toFixed(2) }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-select
        v-model="statusFilter"
        placeholder="结算状态"
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

    <!-- 结算列表 -->
    <el-card v-loading="loading" class="settlements-card">
      <el-table :data="settlements" stripe>
        <el-table-column prop="settlementNo" label="结算单号" width="180">
          <template #default="{ row }">
            <span class="settlement-no">{{ row.settlementNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderId" label="关联订单" width="180">
          <template #default="{ row }">
            <span class="order-link" @click="router.push(`/orders/${row.orderId}`)">
              {{ row.orderId }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="结算金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ row.settlementAmount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结算状态" width="100">
          <template #default="{ row }">
            <el-tag :type="settlementStatusMap[row.settlementStatus]?.type || ''" size="small">
              {{ settlementStatusMap[row.settlementStatus]?.label || row.settlementStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结算时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.settledAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
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

      <el-empty v-if="!loading && settlements.length === 0" description="暂无结算记录" />
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.merchant-settlements-page {
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

  .statistics-row {
    margin-bottom: 16px;

    .stat-card {
      .stat-content {
        text-align: center;
        padding: 16px 0;

        .stat-label {
          font-size: 14px;
          color: #909399;
          margin-bottom: 8px;
        }

        .stat-value {
          font-size: 24px;
          font-weight: bold;
          color: #303133;

          &.warning {
            color: #e6a23c;
          }

          &.success {
            color: #67c23a;
          }
        }
      }
    }
  }

  .filter-card {
    margin-bottom: 16px;
  }

  .settlements-card {
    .settlement-no {
      font-family: monospace;
    }

    .order-link {
      color: #409eff;
      cursor: pointer;
      font-family: monospace;

      &:hover {
        text-decoration: underline;
      }
    }

    .amount {
      font-weight: 600;
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
