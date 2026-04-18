<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import { userApi, type User } from '@/api/user'
import { merchantApi, type Merchant } from '@/api/merchant'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const merchantLoading = ref(false)
const userInfo = ref<User | null>(null)
const merchantInfo = ref<Merchant | null>(null)

// 角色名称映射
const roleNameMap: Record<string, { label: string; type: string }> = {
  'ROLE_USER': { label: '普通用户', type: '' },
  'ROLE_MERCHANT': { label: '商家', type: 'success' },
  'ROLE_ADMIN': { label: '管理员', type: 'warning' },
  'ROLE_SUPER_ADMIN': { label: '超级管理员', type: 'danger' }
}

// 商家状态映射
const merchantStatusMap: Record<string, { label: string; type: string }> = {
  'PENDING': { label: '待审核', type: 'warning' },
  'APPROVED': { label: '已认证', type: 'success' },
  'REJECTED': { label: '已拒绝', type: 'danger' }
}

// 商家类型映射
const merchantTypeMap: Record<string, string> = {
  'PERSONAL': '个人',
  'TEAM': '团队',
  'ENTERPRISE': '企业'
}

const currentRoleInfo = computed(() => {
  // userInfo.value?.role 可能是枚举对象或字符串，统一转为字符串处理
  const role = String(userInfo.value?.role || authStore.userInfo?.role || '')
  return roleNameMap[role] || { label: role, type: '' }
})

// 获取当前用户角色字符串
const currentRoleString = computed(() => {
  return String(userInfo.value?.role || authStore.userInfo?.role || '')
})

const isApprovedMerchant = computed(() => merchantInfo.value?.auditStatus === 'APPROVED')

const loadUserInfo = async () => {
  loading.value = true
  try {
    // 始终从后端获取最新的用户信息，确保角色是最新的
    const authResponse = await authApi.getCurrentUser() as { userId?: string; role?: string; username?: string; email?: string; phone?: string; createdAt?: string }

    if (authResponse.userId) {
      // 直接使用 authResponse 的数据（包含最新角色）
      userInfo.value = {
        userId: authResponse.userId,
        username: authResponse.username || '',
        role: authResponse.role || 'ROLE_USER',
        email: authResponse.email,
        phone: authResponse.phone,
        createdAt: authResponse.createdAt
      } as User

      // 更新 authStore（确保角色信息同步）
      authStore.setUserInfo({
        userId: authResponse.userId,
        username: authResponse.username || '',
        role: authResponse.role || 'ROLE_USER'
      })
    }
  } catch (e: unknown) {
    // API 错误不在这里处理，让 axios 拦截器处理 401 等情况
    // 只在非认证错误时显示提示
    const error = e as { response?: { status?: number } }
    if (error.response?.status !== 401) {
      ElMessage.error('加载用户信息失败')
    }
  } finally {
    loading.value = false
  }
}

const loadMerchantInfo = async (showLoading = false) => {
  if (!authStore.userInfo?.userId) return

  if (showLoading) {
    merchantLoading.value = true
  }

  try {
    // 响应结构为 { code: 200, message: "success", data: Merchant } 或 { code: 404, ... }
    const response = await merchantApi.getByUserId(authStore.userInfo.userId) as unknown as { code?: number; data?: Merchant }
    if (response?.code === 200 && response.data) {
      merchantInfo.value = response.data
    } else {
      // 用户还不是商家（404），不显示提示
      merchantInfo.value = null
    }
  } catch {
    // 用户还不是商家或其他错误，不显示提示
    merchantInfo.value = null
  } finally {
    if (showLoading) {
      merchantLoading.value = false
    }
  }
}

const refreshMerchantInfo = () => {
  loadMerchantInfo(true)
}

const formatDate = (date: string | undefined) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const copyText = (text: string) => {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
}

const handleLogout = async () => {
  try {
    await authApi.logout()
  } catch {
    // ignore
  }
  authStore.clearAuth()
  router.push('/login')
  ElMessage.success('已退出登录')
}

onMounted(async () => {
  await loadUserInfo()
  loadMerchantInfo()
})
</script>

<template>
  <div class="user-center-page">
    <div class="page-header">
      <h2 class="page-title">用户中心</h2>
    </div>

    <el-row :gutter="16">
      <!-- 用户信息 -->
      <el-col :xs="24" :lg="16">
        <el-card v-loading="loading" class="user-card">
          <template #header>
            <div class="card-header">
              <span>基本信息</span>
              <el-tag :type="currentRoleInfo.type">{{ currentRoleInfo.label }}</el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户名">
              {{ userInfo?.username || authStore.userInfo?.username }}
            </el-descriptions-item>
            <el-descriptions-item label="用户ID">
              <span class="copy-text" @click="copyText(userInfo?.userId || '')">
                {{ userInfo?.userId || authStore.userInfo?.userId }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ userInfo?.email || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="手机号">
              {{ userInfo?.phone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="Token有效期">
              {{ userInfo ? '-' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">
              {{ formatDate(userInfo?.createdAt) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 商家信息 -->
        <el-card v-if="merchantInfo" class="merchant-card">
          <template #header>
            <div class="card-header">
              <span>商家信息</span>
              <div class="header-actions">
                <el-tag :type="merchantStatusMap[merchantInfo.auditStatus]?.type">
                  {{ merchantStatusMap[merchantInfo.auditStatus]?.label }}
                </el-tag>
                <el-button text size="small" @click="refreshMerchantInfo" :loading="merchantLoading">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
              </div>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="商家名称">
              {{ merchantInfo.merchantName }}
            </el-descriptions-item>
            <el-descriptions-item label="商家类型">
              {{ merchantTypeMap[merchantInfo.merchantType] || merchantInfo.merchantType }}
            </el-descriptions-item>
            <el-descriptions-item label="联系人">
              {{ merchantInfo.contactName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="联系电话">
              {{ merchantInfo.contactPhone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="联系邮箱">
              {{ merchantInfo.contactEmail || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="入驻时间">
              {{ formatDate(merchantInfo.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item v-if="merchantInfo.auditRemark" label="审核备注" :span="2">
              {{ merchantInfo.auditRemark }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="merchant-actions">
            <el-button type="primary" @click="router.push('/merchant/products')">
              商品管理
            </el-button>
            <el-button @click="router.push('/merchant/orders')">
              订单管理
            </el-button>
            <el-button @click="router.push('/merchant/settlements')">
              结算查询
            </el-button>
          </div>
        </el-card>

        <!-- 未入驻商家提示 - 仅在用户已登录且确实不是商家时才显示 -->
        <el-card v-if="!loading && !merchantInfo" class="apply-card">
          <div class="apply-hint">
            <p class="hint-text">您还没有申请成为商家</p>
            <el-button type="primary" size="small" @click="router.push('/merchant/apply')">
              申请入驻
            </el-button>
          </div>
        </el-card>

        <!-- 商家审核中提示 -->
        <el-card v-if="!loading && merchantInfo && merchantInfo.auditStatus === 'PENDING'" class="apply-card">
          <div class="apply-hint pending">
            <p class="hint-text">商家申请正在审核中，请耐心等待...</p>
            <el-tag type="warning">审核中</el-tag>
          </div>
        </el-card>

        <!-- 商家审核被拒绝提示 -->
        <el-card v-if="!loading && merchantInfo && merchantInfo.auditStatus === 'REJECTED'" class="apply-card">
          <div class="apply-hint rejected">
            <p class="hint-text">商家申请未通过审核{{ merchantInfo.auditRemark ? `：${merchantInfo.auditRemark}` : '' }}</p>
            <el-button type="primary" size="small" @click="router.push('/merchant/apply')">
              重新申请
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 快捷操作 -->
      <el-col :xs="24" :lg="8">
        <el-card class="actions-card">
          <template #header>
            <span>快捷操作</span>
          </template>
          <el-space direction="vertical" :size="12" style="width: 100%">
            <el-button style="width: 100%" @click="router.push('/orders')">
              <el-icon><ShoppingCart /></el-icon>
              我的订单
            </el-button>
            <el-button v-if="isApprovedMerchant" style="width: 100%" @click="router.push('/merchant/products')">
              <el-icon><Goods /></el-icon>
              商品管理
            </el-button>
            <el-button v-if="authStore.isAdmin || authStore.isMerchant" style="width: 100%" @click="router.push('/admin')">
              <el-icon><Setting /></el-icon>
              管理后台
            </el-button>
          </el-space>
        </el-card>

        <el-card class="security-card">
          <template #header>
            <span>账号安全</span>
          </template>
          <el-space direction="vertical" :size="12" style="width: 100%">
            <el-button style="width: 100%">修改密码</el-button>
            <el-button style="width: 100%">绑定邮箱</el-button>
            <el-button style="width: 100%">绑定手机</el-button>
          </el-space>
        </el-card>

        <el-card class="logout-card">
          <el-button type="danger" plain style="width: 100%" @click="handleLogout">
            退出登录
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.user-center-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;

  .page-header {
    margin-bottom: 24px;

    .page-title {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: #303133;
    }
  }

  .user-card,
  .merchant-card,
  .apply-card {
    margin-bottom: 16px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-actions {
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }

    .copy-text {
      cursor: pointer;
      font-family: monospace;
      color: #409eff;

      &:hover {
        text-decoration: underline;
      }
    }

    .merchant-actions {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #ebeef5;
      display: flex;
      gap: 8px;
    }

    .apply-hint {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 24px;
      gap: 16px;

      .hint-text {
        margin: 0;
        color: #606266;
        font-size: 14px;
      }

      &.pending {
        .hint-text {
          color: #E6A23C;
        }
      }

      &.rejected {
        .hint-text {
          color: #F56C6C;
        }
      }
    }
  }

  .actions-card,
  .security-card,
  .logout-card {
    margin-bottom: 16px;
  }

  .logout-card {
    :deep(.el-card__header) {
      display: none;
    }
  }
}
</style>
