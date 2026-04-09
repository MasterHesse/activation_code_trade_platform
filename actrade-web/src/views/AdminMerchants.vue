<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { merchantApi, type Merchant, type MerchantAuditStatus } from '@/api/merchant'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const merchants = ref<Merchant[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref<MerchantAuditStatus | ''>('')

// 弹窗
const auditDialogVisible = ref(false)
const auditRemark = ref('')
const pendingMerchant = ref<Merchant | null>(null)

const statusOptions: { label: string; value: string }[] = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' }
]

const statusMap: Record<string, { label: string; type: string }> = {
  'PENDING': { label: '待审核', type: 'warning' },
  'APPROVED': { label: '已通过', type: 'success' },
  'REJECTED': { label: '已拒绝', type: 'danger' }
}

const typeMap: Record<string, string> = {
  'PERSONAL': '个人',
  'TEAM': '团队',
  'ENTERPRISE': '企业'
}

const checkAdmin = () => {
  if (!authStore.isAdmin) {
    ElMessage.error('无权限访问此页面')
    router.push('/')
  }
}

const loadMerchants = async () => {
  loading.value = true
  try {
    const response = await merchantApi.list()

    let data: Merchant[] = Array.isArray(response) ? response : []
    if (statusFilter.value) {
      data = data.filter(m => m.auditStatus === statusFilter.value)
    }

    // 分页
    const start = currentPage.value - 1
    const end = start + pageSize.value
    merchants.value = data.slice(start, end)
    total.value = data.length
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '加载商家列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  currentPage.value = 1
  loadMerchants()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadMerchants()
}

const handleAudit = (merchant: Merchant) => {
  pendingMerchant.value = merchant
  auditRemark.value = ''
  auditDialogVisible.value = true
}

const handleApprove = async () => {
  if (!pendingMerchant.value) return

  try {
    await merchantApi.audit(pendingMerchant.value.merchantId, {
      auditStatus: 'APPROVED',
      auditRemark: auditRemark.value || '审核通过'
    })

    ElMessage.success('商家审核已通过')
    auditDialogVisible.value = false
    loadMerchants()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '操作失败')
  }
}

const handleReject = async () => {
  if (!pendingMerchant.value) return

  if (!auditRemark.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }

  try {
    await merchantApi.audit(pendingMerchant.value.merchantId, {
      auditStatus: 'REJECTED',
      auditRemark: auditRemark.value
    })

    ElMessage.success('商家申请已拒绝')
    auditDialogVisible.value = false
    loadMerchants()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '操作失败')
  }
}

onMounted(() => {
  checkAdmin()
  loadMerchants()
})
</script>

<template>
  <div class="admin-merchants-page">
    <div class="page-header">
      <h2 class="page-title">商家审核</h2>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-select v-model="statusFilter" placeholder="审核状态" clearable @change="handleFilter">
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
    </el-card>

    <!-- 商家列表 -->
    <el-card v-loading="loading" class="merchants-card">
      <el-table :data="merchants" stripe>
        <el-table-column prop="merchantName" label="商家名称" min-width="150" />
        <el-table-column label="商家类型" width="100">
          <template #default="{ row }">
            {{ typeMap[row.merchantType] || row.merchantType }}
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="contactEmail" label="邮箱" width="180" />
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.auditStatus]?.type || ''" size="small">
              {{ statusMap[row.auditStatus]?.label || row.auditStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="审核备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="申请时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.auditStatus === 'PENDING'">
              <el-button size="small" type="success" @click="handleAudit(row)">审核</el-button>
            </template>
            <template v-else>
              <el-button size="small" @click="handleAudit(row)">查看</el-button>
            </template>
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

      <el-empty v-if="!loading && merchants.length === 0" description="暂无商家申请" />
    </el-card>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" :title="pendingMerchant?.auditStatus === 'PENDING' ? '审核商家' : '查看详情'" width="600px">
      <template v-if="pendingMerchant">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商家名称">{{ pendingMerchant.merchantName }}</el-descriptions-item>
          <el-descriptions-item label="商家类型">{{ typeMap[pendingMerchant.merchantType] }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ pendingMerchant.contactName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ pendingMerchant.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系邮箱" :span="2">{{ pendingMerchant.contactEmail || '-' }}</el-descriptions-item>
          <el-descriptions-item label="资质信息" :span="2">
            <pre class="license-info">{{ pendingMerchant.licenseInfo || '无' }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间" :span="2">{{ pendingMerchant.createdAt }}</el-descriptions-item>
        </el-descriptions>

        <el-divider v-if="pendingMerchant.auditStatus === 'PENDING'" />

        <el-form v-if="pendingMerchant.auditStatus === 'PENDING'" label-position="top">
          <el-form-item label="审核备注">
            <el-input
              v-model="auditRemark"
              type="textarea"
              :rows="3"
              placeholder="请填写审核备注（拒绝时必填）"
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="auditDialogVisible = false">关闭</el-button>
        <template v-if="pendingMerchant?.auditStatus === 'PENDING'">
          <el-button type="danger" @click="handleReject">拒绝</el-button>
          <el-button type="success" @click="handleApprove">通过</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.admin-merchants-page {
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

  .merchants-card {
    .license-info {
      margin: 0;
      white-space: pre-wrap;
      font-size: 12px;
      max-height: 100px;
      overflow: auto;
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: center;
    }
  }
}
</style>
