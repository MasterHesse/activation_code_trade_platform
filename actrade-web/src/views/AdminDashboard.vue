<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api/user'
import { merchantApi, type Merchant } from '@/api/merchant'
import { productApi } from '@/api/product'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const stats = ref({
  userCount: 0,
  merchantCount: 0,
  productCount: 0,
  orderCount: 0,
  pendingMerchantCount: 0
})

const isAdmin = computed(() => authStore.isAdmin)

// 检查管理员权限
const checkAdmin = () => {
  if (!isAdmin.value) {
    ElMessage.error('无权限访问此页面')
    router.push('/')
    return false
  }
  return true
}

const loadStats = async () => {
  loading.value = true
  try {
    // 并行加载统计数据
    const [usersRes, merchantsRes, productsRes] = await Promise.allSettled([
      userApi.page({ size: 1 }),
      merchantApi.list(),
      productApi.list({ size: 1 })
    ])

    if (usersRes.status === 'fulfilled') {
      const usersData = usersRes.value as { data?: { data?: { totalElements?: number } } }
      stats.value.userCount = usersData?.data?.data?.totalElements || 0
    }
    if (merchantsRes.status === 'fulfilled') {
      const merchantsData = merchantsRes.value as unknown as Merchant[]
      const merchants: Merchant[] = Array.isArray(merchantsData) ? merchantsData : []
      stats.value.merchantCount = merchants.length
      stats.value.pendingMerchantCount = merchants.filter((m) => m.auditStatus === 'PENDING').length
    }
    if (productsRes.status === 'fulfilled') {
      const productsData = productsRes.value as { data?: { data?: { totalElements?: number } } }
      stats.value.productCount = productsData?.data?.data?.totalElements || 0
    }

    // 订单统计：由于没有管理员订单列表 API，暂时设为预估数
    // TODO: 后端可添加管理员订单统计接口
    stats.value.orderCount = 0
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

// 快捷菜单
const adminMenus = [
  {
    title: '用户管理',
    icon: 'User',
    description: '管理平台用户账号',
    route: '/admin/users',
    color: '#409eff'
  },
  {
    title: '商家审核',
    icon: 'Shop',
    description: '审核商家入驻申请',
    route: '/admin/merchants',
    color: '#67c23a',
    badge: computed(() => stats.value.pendingMerchantCount > 0 ? stats.value.pendingMerchantCount : null)
  },
  {
    title: '商品管理',
    icon: 'Goods',
    description: '管理平台商品',
    route: '/products',
    color: '#e6a23c'
  },
  {
    title: '订单管理',
    icon: 'Document',
    description: '查看平台订单',
    route: '/orders',
    color: '#f56c6c'
  }
]

onMounted(() => {
  if (checkAdmin()) {
    loadStats()
  }
})
</script>

<template>
  <div class="admin-dashboard">
    <div class="page-header">
      <h2 class="page-title">管理后台</h2>
      <div class="header-info">
        <span class="welcome">欢迎，{{ authStore.userInfo?.username }}</span>
        <el-tag :type="isAdmin ? 'warning' : ''">{{ authStore.userInfo?.role?.replace('ROLE_', '') }}</el-tag>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#409eff"><User /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.userCount }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#67c23a"><Shop /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.merchantCount }}</div>
              <div class="stat-label">商家总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#e6a23c"><Goods /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.productCount }}</div>
              <div class="stat-label">商品总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#f56c6c"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.orderCount }}</div>
              <div class="stat-label">订单总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待处理提醒 -->
    <el-card v-if="stats.pendingMerchantCount > 0" class="alert-card">
      <template #header>
        <div class="alert-header">
          <span><el-icon><Warning /></el-icon> 待处理事项</span>
        </div>
      </template>
      <el-alert
        :title="`您有 ${stats.pendingMerchantCount} 个商家入驻申请待审核`"
        type="warning"
        show-icon
        :closable="false"
      >
        <template #default>
          <el-button type="warning" size="small" @click="router.push('/admin/merchants')">
            立即处理
          </el-button>
        </template>
      </el-alert>
    </el-card>

    <!-- 快捷入口 -->
    <el-card class="menus-card">
      <template #header>
        <span>快捷管理</span>
      </template>
      <el-row :gutter="16">
        <el-col v-for="menu in adminMenus" :key="menu.route" :span="6">
          <div
            class="menu-item"
            :style="{ borderColor: menu.color }"
            @click="router.push(menu.route)"
          >
            <el-icon class="menu-icon" :color="menu.color" :size="32">
              <component :is="menu.icon" />
            </el-icon>
            <div class="menu-info">
              <div class="menu-title">
                {{ menu.title }}
                <el-badge v-if="menu.badge" :value="menu.badge.value" type="danger" />
              </div>
              <div class="menu-desc">{{ menu.description }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 功能说明 -->
    <el-card class="info-card">
      <template #header>
        <span>功能说明</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户管理">
          管理系统中的所有用户账号，包括查看、编辑用户信息、修改用户角色、锁定/解锁用户等操作。
        </el-descriptions-item>
        <el-descriptions-item label="商家审核">
          审核商家的入驻申请，包括个人商家、团队商家和企业商家。审核通过后商家可发布商品。
        </el-descriptions-item>
        <el-descriptions-item label="商品管理">
          查看和管理平台上的所有商品，包括商品信息审核、价格调整、上下架管理等。
        </el-descriptions-item>
        <el-descriptions-item label="订单管理">
          查看平台所有订单，处理异常订单，协助用户解决问题。
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.admin-dashboard {
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

    .header-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .welcome {
        color: #606266;
      }
    }
  }

  .stats-row {
    margin-bottom: 24px;

    .stat-card {
      .stat-content {
        display: flex;
        align-items: center;
        gap: 16px;

        .stat-icon {
          font-size: 48px;
        }

        .stat-info {
          .stat-value {
            font-size: 28px;
            font-weight: bold;
            color: #303133;
          }

          .stat-label {
            font-size: 14px;
            color: #909399;
          }
        }
      }
    }
  }

  .alert-card {
    margin-bottom: 24px;

    .alert-header {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .menus-card {
    margin-bottom: 24px;

    .menu-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 24px 16px;
      border: 2px solid transparent;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s;
      text-align: center;

      &:hover {
        background: #f5f7fa;
        transform: translateY(-4px);
      }

      .menu-icon {
        margin-bottom: 12px;
      }

      .menu-info {
        .menu-title {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 4px;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
        }

        .menu-desc {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }

  .info-card {
    :deep(.el-descriptions__label) {
      width: 120px;
    }
  }
}
</style>
