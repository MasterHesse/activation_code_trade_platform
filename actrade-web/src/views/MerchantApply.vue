<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { merchantApi, type CreateMerchantRequest, type Merchant, type MerchantAuditStatus } from '@/api/merchant'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const loading = ref(false)
const checkLoading = ref(true)
const existingMerchant = ref<Merchant | null>(null)

const formData = ref<CreateMerchantRequest>({
  userId: '',  // 将在提交时自动填充
  merchantName: '',
  merchantType: 'PERSONAL',  // 修复：后端枚举值为 PERSONAL，不是 INDIVIDUAL
  contactName: '',
  contactEmail: '',
  contactPhone: '',
  licenseInfo: ''
})

const rules = {
  merchantName: [
    { required: true, message: '请输入商家名称', trigger: 'blur' },
    { min: 2, max: 50, message: '商家名称2-50个字符', trigger: 'blur' }
  ],
  merchantType: [
    { required: true, message: '请选择商家类型', trigger: 'change' }
  ],
  contactPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ],
  contactEmail: [
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

const merchantTypeOptions = [
  { label: '个人商家', value: 'PERSONAL' },
  { label: '团队商家', value: 'TEAM' },
  { label: '企业商家', value: 'ENTERPRISE' }
]

const getAuditStatusText = (status: MerchantAuditStatus) => {
  const map: Record<MerchantAuditStatus, string> = {
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  }
  return map[status] || status
}

const getAuditStatusType = (status: MerchantAuditStatus) => {
  const map: Record<MerchantAuditStatus, string> = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return map[status] || ''
}

const checkExistingMerchant = async () => {
  if (!authStore.userInfo?.userId) {
    checkLoading.value = false
    return
  }

  try {
    const response = await merchantApi.getByUserId(authStore.userInfo.userId) as unknown as Merchant
    existingMerchant.value = response
  } catch {
    // 用户尚未申请商家
    existingMerchant.value = null
  } finally {
    checkLoading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    // 验证用户已登录
    if (!authStore.userInfo?.userId) {
      ElMessage.error('请先登录')
      router.push('/login')
      return
    }

    loading.value = true
    try {
      // 构造请求数据，自动注入 userId
      const requestData: CreateMerchantRequest = {
        ...formData.value,
        userId: authStore.userInfo.userId
      }

      await merchantApi.apply(requestData)
      ElMessage.success('商家申请已提交，请等待审核')
      router.push('/products')
    } catch (e: unknown) {
      const error = e as { response?: { data?: { message?: string } }; message?: string }
      ElMessage.error(error.response?.data?.message || error.message || '申请失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  checkExistingMerchant()
})
</script>

<template>
  <div class="merchant-apply-page">
    <div class="page-container">
      <div class="page-header">
        <h2 class="page-title">商家入驻申请</h2>
        <p class="page-desc">填写商家信息，完成入驻后可发布商品进行销售</p>
      </div>
      
      <div v-if="checkLoading" v-loading="checkLoading" class="check-loading" />
      
      <!-- 已有商家申请记录 -->
      <el-card v-else-if="existingMerchant" class="existing-record">
        <div class="record-header">
          <el-icon size="48" color="#909399"><Warning /></el-icon>
          <h3>您已是商家</h3>
        </div>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="商家名称">
            {{ existingMerchant?.merchantName }}
          </el-descriptions-item>
          <el-descriptions-item label="商家类型">
            {{ merchantTypeOptions.find(o => o.value === existingMerchant?.merchantType)?.label || existingMerchant?.merchantType }}
          </el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <el-tag :type="getAuditStatusType(existingMerchant?.auditStatus)">
              {{ getAuditStatusText(existingMerchant?.auditStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="联系人">
            {{ existingMerchant?.contactName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话" :span="2">
            {{ existingMerchant?.contactPhone || '-' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="existingMerchant?.auditRemark" label="审核备注" :span="2">
            {{ existingMerchant?.auditRemark }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="record-actions">
          <el-button type="primary" @click="router.push('/merchant/products')">
            管理商品
          </el-button>
          <el-button @click="router.push('/products')">
            返回商城
          </el-button>
        </div>
      </el-card>
      
      <!-- 新申请表单 -->
      <el-card v-else class="apply-form">
        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-position="top"
        >
          <el-form-item label="商家类型" prop="merchantType">
            <el-radio-group v-model="formData.merchantType" size="large">
              <el-radio-button
                v-for="opt in merchantTypeOptions"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="商家名称" prop="merchantName">
            <el-input
              v-model="formData.merchantName"
              placeholder="请输入商家名称"
              size="large"
              maxlength="50"
              show-word-limit
            />
          </el-form-item>
          
          <el-form-item label="联系人姓名" prop="contactName">
            <el-input
              v-model="formData.contactName"
              placeholder="请输入联系人姓名"
              size="large"
            />
          </el-form-item>
          
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input
              v-model="formData.contactPhone"
              placeholder="请输入联系电话"
              size="large"
            />
          </el-form-item>
          
          <el-form-item label="联系邮箱" prop="contactEmail">
            <el-input
              v-model="formData.contactEmail"
              placeholder="请输入联系邮箱（选填）"
              size="large"
            />
          </el-form-item>
          
          <el-form-item v-if="formData.merchantType === 'ENTERPRISE'" label="营业执照信息" prop="licenseInfo">
            <el-input
              v-model="formData.licenseInfo"
              type="textarea"
              :rows="3"
              placeholder="请输入营业执照信息"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              style="width: 200px"
              @click="handleSubmit"
            >
              提交申请
            </el-button>
            <el-button size="large" @click="router.push('/products')">
              稍后再说
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.merchant-apply-page {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
  
  .page-container {
    max-width: 800px;
    margin: 0 auto;
    
    .page-header {
      margin-bottom: 24px;
      
      .page-title {
        margin: 0 0 8px;
        font-size: 24px;
        font-weight: 600;
        color: #303133;
      }
      
      .page-desc {
        margin: 0;
        color: #909399;
        font-size: 14px;
      }
    }
    
    .check-loading {
      min-height: 200px;
    }
    
    .existing-record {
      .record-header {
        text-align: center;
        padding: 20px 0;
        
        h3 {
          margin: 16px 0 0;
          font-size: 18px;
          color: #303133;
        }
      }
      
      .record-actions {
        margin-top: 24px;
        text-align: center;
        
        .el-button {
          margin: 0 8px;
        }
      }
    }
    
    .apply-form {
      .el-form-item {
        max-width: 500px;
      }
      
      .el-radio-group {
        :deep(.el-radio-button__inner) {
          min-width: 120px;
        }
      }
    }
  }
}
</style>
