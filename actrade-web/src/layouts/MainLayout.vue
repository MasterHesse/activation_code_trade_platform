<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import {
  Fold,
  Expand,
  UserFilled,
  SwitchButton,
  Coin
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isCollapse = ref(false)

const defaultActive = computed(() => route.path)

const handleLogout = async () => {
  try {
    await authApi.logout()
  } catch (e) {
    // 忽略错误
  }
  authStore.clearAuth()
  router.push({ name: 'Login' })
}

const menuItems = computed(() => {
  const items: { path: string; title: string; icon: string }[] = [
    { path: '/products', title: '商品列表', icon: 'Shop' },
    { path: '/orders', title: '我的订单', icon: 'List' }
  ]

  // 如果是商家角色，添加商家管理入口（管理员不应看到商家管理）
  // 确保 userInfo 存在且 role 为 ROLE_MERCHANT
  if (authStore.userInfo && authStore.userInfo.role === 'ROLE_MERCHANT') {
    items.push(
      { path: '/merchant/products', title: '商品管理', icon: 'Goods' }
    )
    items.push(
      { path: '/merchant/orders', title: '商家订单', icon: 'List' }
    )
    items.push(
      { path: '/merchant/settlements', title: '结算查询', icon: 'Coin' }
    )
  }

  // 如果是管理员，添加管理后台入口（管理员不显示商家管理）
  if (authStore.userInfo && authStore.userInfo.role === 'ROLE_ADMIN') {
    items.push(
      { path: '/admin', title: '管理后台', icon: 'Setting' }
    )
  }

  items.push({ path: '/user-center', title: '用户中心', icon: 'User' })

  // 引用 Coin 图标以消除未使用警告
  void Coin

  return items
})
</script>

<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '200px'">
      <div class="logo">
        <span v-if="!isCollapse">ACTrade</span>
        <span v-else>AC</span>
      </div>
      
      <el-menu
        :default-active="defaultActive"
        :collapse="isCollapse"
        :router="true"
        class="sidebar-menu"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header>
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
        </div>
        
        <div class="header-right">
          <el-dropdown @command="handleLogout">
            <span class="user-info">
              <el-avatar :size="32" :icon="UserFilled" />
              <span class="username">{{ authStore.userInfo?.username || '用户' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 页面内容 -->
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
}

.el-aside {
  background: #304156;
  transition: width 0.3s;
  
  .logo {
    height: 60px;
    line-height: 60px;
    text-align: center;
    background: #2b3a4a;
    color: #fff;
    font-size: 20px;
    font-weight: bold;
  }
  
  .sidebar-menu {
    border-right: none;
    background: transparent;
    
    :deep(.el-menu-item) {
      color: #bfcbd9;
      
      &:hover, &.is-active {
        background: #263445;
        color: #409eff;
      }
    }
  }
}

.el-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  
  .collapse-btn {
    font-size: 20px;
    cursor: pointer;
    color: #606266;
    
    &:hover {
      color: #409eff;
    }
  }
  
  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      
      .username {
        color: #606266;
      }
    }
  }
}

.el-main {
  background: #f0f2f5;
  padding: 16px;
}
</style>
