<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { merchantApi, type Merchant } from '@/api/merchant'
import { productApi, type Product } from '@/api/product'
import {
  activationCodeApi,
  type ActivationCode,
  type ActivationCodeStatus,
  type BatchCreateActivationCodeItem
} from '@/api/activation'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const merchant = ref<Merchant | null>(null)
const products = ref<Product[]>([])
const codes = ref<ActivationCode[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref<ActivationCodeStatus | ''>('')
const productFilter = ref<string>('')

// 弹窗
const batchImportDialogVisible = ref(false)
const batchImportLoading = ref(false)
const batchImportText = ref('')
const selectedProductId = ref('')
const batchNo = ref('')

const statusOptions: { label: string; value: string }[] = [
  { label: '全部', value: '' },
  { label: '可用', value: 'AVAILABLE' },
  { label: '已锁定', value: 'LOCKED' },
  { label: '已售出', value: 'SOLD' },
  { label: '已作废', value: 'VOID' }
]

const statusMap: Record<string, { label: string; type: string }> = {
  'AVAILABLE': { label: '可用', type: 'success' },
  'LOCKED': { label: '已锁定', type: 'warning' },
  'SOLD': { label: '已售出', type: 'primary' },
  'VOID': { label: '已作废', type: 'info' }
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

const loadCodes = async () => {
  if (!merchant.value?.merchantId) return

  loading.value = true
  try {
    const response = await activationCodeApi.page({
      productId: productFilter.value || undefined,
      status: statusFilter.value || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    }) as { content?: ActivationCode[]; totalElements?: number }

    codes.value = response?.content || []
    total.value = response?.totalElements || 0
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '加载激活码失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  currentPage.value = 1
  loadCodes()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadCodes()
}

// 批量导入激活码
const handleBatchImport = async () => {
  if (!selectedProductId.value) {
    ElMessage.warning('请选择商品')
    return
  }

  if (!batchNo.value.trim()) {
    ElMessage.warning('请输入批次号')
    return
  }

  const lines = batchImportText.value.trim().split('\n').filter(line => line.trim())
  if (lines.length === 0) {
    ElMessage.warning('请输入激活码，每行一个')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要导入 ${lines.length} 个激活码吗？`,
      '确认导入',
      { confirmButtonText: '确认导入', cancelButtonText: '取消', type: 'info' }
    )

    batchImportLoading.value = true
    const items: BatchCreateActivationCodeItem[] = lines.map((line, index) => {
      const code = line.trim()
      return {
        batchNo: batchNo.value.trim(),
        codeValueEncrypted: code,
        codeValueMasked: code.substring(0, 3) + '****' + code.substring(code.length - 3),
        codeValueHash: `hash_${code}_${Date.now()}_${index}`,
        remark: `批次: ${batchNo.value}`
      }
    })

    await activationCodeApi.batchCreate({
      productId: selectedProductId.value,
      merchantId: merchant.value!.merchantId,
      items
    })

    ElMessage.success(`成功导入 ${items.length} 个激活码`)
    batchImportDialogVisible.value = false
    batchImportText.value = ''
    batchNo.value = ''
    loadCodes()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    if (err.response?.data?.message) {
      ElMessage.error(err.response.data.message)
    } else if (err.message !== 'cancel') {
      ElMessage.error('导入失败')
    }
  } finally {
    batchImportLoading.value = false
  }
}

// 作废激活码
const handleVoid = async (code: ActivationCode) => {
  try {
    await ElMessageBox.confirm(
      `确定要作废激活码「${code.codeValueMasked}」吗？`,
      '确认作废',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )

    await activationCodeApi.voidCode(code.codeId)
    ElMessage.success('激活码已作废')
    loadCodes()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    if (err.response?.data?.message) {
      ElMessage.error(err.response.data.message)
    } else if (err.message !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 复制激活码
const copyCode = (code: ActivationCode) => {
  navigator.clipboard.writeText(code.codeValueMasked)
  ElMessage.success('激活码已复制')
}

onMounted(async () => {
  await loadMerchant()
  loadProducts()
  loadCodes()
})
</script>

<template>
  <div class="activation-codes-page">
    <div class="page-header">
      <h2 class="page-title">激活码管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="batchImportDialogVisible = true">
          批量导入
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-select
            v-model="productFilter"
            placeholder="选择商品"
            clearable
            filterable
            @change="handleFilter"
          >
            <el-option
              v-for="p in products"
              :key="p.productId"
              :label="p.name"
              :value="p.productId"
            />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-select
            v-model="statusFilter"
            placeholder="状态"
            clearable
            @change="handleFilter"
          >
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-col>
      </el-row>
    </el-card>

    <!-- 激活码列表 -->
    <el-card v-loading="loading" class="codes-card">
      <el-table :data="codes" stripe>
        <el-table-column prop="codeValueMasked" label="激活码" width="200">
          <template #default="{ row }">
            <span class="code-value" @click="copyCode(row)">{{ row.codeValueMasked }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || ''" size="small">
              {{ statusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="过期时间" width="160">
          <template #default="{ row }">
            {{ row.expiredAt || '永久有效' }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'AVAILABLE'"
              type="warning"
              size="small"
              @click="handleVoid(row)"
            >
              作废
            </el-button>
            <el-button
              v-if="row.status === 'SOLD'"
              size="small"
              @click="copyCode(row)"
            >
              复制
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

      <el-empty v-if="!loading && codes.length === 0" description="暂无激活码">
        <el-button type="primary" @click="batchImportDialogVisible = true">
          批量导入
        </el-button>
      </el-empty>
    </el-card>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="batchImportDialogVisible" title="批量导入激活码" width="600px">
      <el-form label-position="top">
        <el-form-item label="选择商品" required>
          <el-select
            v-model="selectedProductId"
            placeholder="请选择商品"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="p in products"
              :key="p.productId"
              :label="p.name"
              :value="p.productId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="批次号" required>
          <el-input v-model="batchNo" placeholder="请输入批次号，如 BATCH_20240101" />
        </el-form-item>

        <el-form-item label="激活码列表" required>
          <el-input
            v-model="batchImportText"
            type="textarea"
            :rows="10"
            placeholder="请输入激活码，每行一个"
          />
          <div class="tip">提示：每行一个激活码，导入后将自动生成掩码显示</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="batchImportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchImportLoading" @click="handleBatchImport">
          确认导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.activation-codes-page {
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

  .codes-card {
    .code-value {
      font-family: monospace;
      color: #409eff;
      cursor: pointer;

      &:hover {
        text-decoration: underline;
      }
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: center;
    }
  }

  .tip {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
  }
}
</style>
