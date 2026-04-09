<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { merchantApi, type Merchant } from '@/api/merchant'
import { productApi, type Product } from '@/api/product'
import {
  activationToolApi,
  activationToolVersionApi,
  type ActivationTool,
  type ActivationToolVersion
} from '@/api/activation'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const merchant = ref<Merchant | null>(null)
const products = ref<Product[]>([])
const tools = ref<ActivationTool[]>([])
const selectedTool = ref<ActivationTool | null>(null)
const versions = ref<ActivationToolVersion[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const statusMap: Record<string, { label: string; type: string }> = {
  'ENABLED': { label: '启用', type: 'success' },
  'DISABLED': { label: '禁用', type: 'info' }
}

const auditStatusMap: Record<string, { label: string; type: string }> = {
  'PENDING': { label: '待审核', type: 'warning' },
  'APPROVED': { label: '已通过', type: 'success' },
  'REJECTED': { label: '已拒绝', type: 'danger' }
}

const versionStatusMap: Record<string, { label: string; type: string }> = {
  'DRAFT': { label: '草稿', type: 'info' },
  'PENDING': { label: '待审核', type: 'warning' },
  'ENABLED': { label: '启用', type: 'success' },
  'DISABLED': { label: '禁用', type: 'info' },
  'REJECTED': { label: '已拒绝', type: 'danger' }
}

const scanStatusMap: Record<string, { label: string; type: string }> = {
  'PENDING': { label: '待扫描', type: 'warning' },
  'SAFE': { label: '安全', type: 'success' },
  'DANGEROUS': { label: '危险', type: 'danger' },
  'FAILED': { label: '失败', type: 'info' }
}

const loadMerchant = async () => {
  if (!authStore.userInfo?.userId) {
    ElMessage.error('请先登录')
    router.push('/login')
    return
  }

  try {
    const response = await merchantApi.getByUserId(authStore.userInfo.userId) as unknown as Merchant
    merchant.value = response
    if (merchant.value?.auditStatus !== 'APPROVED') {
      ElMessage.warning('您的商家资质尚未审核通过')
      router.push('/merchant/apply')
    }
  } catch {
    ElMessage.warning('您还未申请成为商家')
    router.push('/merchant/apply')
  }
}

const loadProducts = async () => {
  if (!merchant.value?.merchantId) return

  try {
    const response = await productApi.list({
      merchantId: merchant.value.merchantId,
      size: 100
    }) as { content?: Product[] }
    products.value = response?.content || []
  } catch {
    // ignore
  }
}

const loadTools = async () => {
  if (!merchant.value?.merchantId) return

  loading.value = true
  try {
    const response = await activationToolApi.page({
      merchantId: merchant.value.merchantId,
      page: currentPage.value - 1,
      size: pageSize.value
    }) as { content?: ActivationTool[]; totalElements?: number }

    tools.value = response?.content || []
    total.value = response?.totalElements || 0
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '加载激活工具失败')
  } finally {
    loading.value = false
  }
}

const loadVersions = async (toolId: string) => {
  try {
    const response = await activationToolVersionApi.page({ toolId, size: 50 }) as { content?: ActivationToolVersion[] }
    versions.value = response?.content || []
  } catch {
    versions.value = []
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadTools()
}

const getProductName = (productId: string) => {
  const product = products.value.find(p => p.productId === productId)
  return product?.name || productId
}

const viewVersions = (tool: ActivationTool) => {
  selectedTool.value = tool
  loadVersions(tool.toolId)
}

onMounted(async () => {
  await loadMerchant()
  loadProducts()
  loadTools()
})
</script>

<template>
  <div class="activation-tools-page">
    <div class="page-header">
      <h2 class="page-title">激活工具管理</h2>
      <div class="header-actions">
        <el-button type="primary" disabled>
          创建工具（暂未开放）
        </el-button>
      </div>
    </div>

    <!-- 工具列表 -->
    <el-card v-loading="loading" class="tools-card">
      <el-table :data="tools" stripe>
        <el-table-column prop="toolName" label="工具名称" min-width="150" />
        <el-table-column label="关联商品" width="150">
          <template #default="{ row }">
            <span class="product-name">{{ getProductName(row.productId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="当前版本" width="120">
          <template #default="{ row }">
            {{ row.currentVersionId || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="工具状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.toolStatus]?.type || ''" size="small">
              {{ statusMap[row.toolStatus]?.label || row.toolStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="auditStatusMap[row.auditStatus]?.type || ''" size="small">
              {{ auditStatusMap[row.auditStatus]?.label || row.auditStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewVersions(row)">版本</el-button>
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

      <el-empty v-if="!loading && tools.length === 0" description="暂无激活工具">
        <template #image>
          <el-icon :size="60" color="#909399"><Tools /></el-icon>
        </template>
        <div class="empty-tip">激活工具需平台审核通过后方可使用</div>
      </el-empty>
    </el-card>

    <!-- 版本详情弹窗 -->
    <el-dialog
      v-model="versionsDialogVisible"
      :title="`${selectedTool?.toolName} - 版本列表`"
      width="900px"
    >
      <el-table :data="versions" stripe>
        <el-table-column prop="versionName" label="版本号" width="120" />
        <el-table-column label="运行时" width="120">
          <template #default="{ row }">
            {{ row.runtimeOs }} / {{ row.runtimeArch }} / {{ row.runtimeType }}
          </template>
        </el-table-column>
        <el-table-column prop="entrypoint" label="入口文件" width="150" show-overflow-tooltip />
        <el-table-column label="资源限制" width="150">
          <template #default="{ row }">
            {{ row.timeoutSeconds }}s / {{ row.maxMemoryMb }}MB
          </template>
        </el-table-column>
        <el-table-column label="扫描状态" width="100">
          <template #default="{ row }">
            <el-tag :type="scanStatusMap[row.scanStatus]?.type || ''" size="small">
              {{ scanStatusMap[row.scanStatus]?.label || row.scanStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本状态" width="100">
          <template #default="{ row }">
            <el-tag :type="versionStatusMap[row.status]?.type || ''" size="small">
              {{ versionStatusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
      </el-table>

      <el-empty v-if="versions.length === 0" description="暂无版本" />
    </el-dialog>

    <el-empty v-if="!loading && tools.length === 0" />
  </div>
</template>

<script lang="ts">
const versionsDialogVisible = ref(false)
export default {}
</script>

<style scoped lang="scss">
.activation-tools-page {
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

  .tools-card {
    .product-name {
      color: #606266;
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: center;
    }

    .empty-tip {
      color: #909399;
      font-size: 14px;
      margin-top: 8px;
    }
  }
}
</style>
