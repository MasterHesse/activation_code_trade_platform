import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false, title: '注册' }
  },
  // 支付跳转页面（独立页面，不使用 MainLayout）
  {
    path: '/payment-gateway',
    name: 'PaymentGateway',
    component: () => import('@/views/PaymentGateway.vue'),
    meta: { requiresAuth: false, title: '支付跳转' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/products'
      },
      // 用户端页面
      {
        path: 'products',
        name: 'Products',
        component: () => import('@/views/Products.vue'),
        meta: { title: '商品列表', requiresAuth: true }
      },
      {
        path: 'products/:id',
        name: 'ProductDetail',
        component: () => import('@/views/ProductDetail.vue'),
        meta: { title: '商品详情', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/Orders.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/OrderDetail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'user-center',
        name: 'UserCenter',
        component: () => import('@/views/UserCenter.vue'),
        meta: { title: '用户中心', requiresAuth: true }
      },
      // 商家端页面
      {
        path: 'merchant/apply',
        name: 'MerchantApply',
        component: () => import('@/views/MerchantApply.vue'),
        meta: { title: '商家入驻', requiresAuth: true }
      },
      {
        path: 'merchant/products',
        name: 'MerchantProducts',
        component: () => import('@/views/MerchantProducts.vue'),
        meta: { title: '商品管理', requiresAuth: true }
      },
      {
        path: 'merchant/orders',
        name: 'MerchantOrders',
        component: () => import('@/views/MerchantOrders.vue'),
        meta: { title: '商家订单', requiresAuth: true }
      },
      {
        path: 'merchant/settlements',
        name: 'MerchantSettlements',
        component: () => import('@/views/MerchantSettlements.vue'),
        meta: { title: '结算查询', requiresAuth: true }
      },
      {
        path: 'merchant/codes',
        name: 'MerchantActivationCodes',
        component: () => import('@/views/MerchantActivationCodes.vue'),
        meta: { title: '激活码管理', requiresAuth: true }
      },
      {
        path: 'merchant/tools',
        name: 'MerchantActivationTools',
        component: () => import('@/views/MerchantActivationTools.vue'),
        meta: { title: '激活工具', requiresAuth: true }
      },
      // 管理端页面
      {
        path: 'admin',
        name: 'AdminDashboard',
        component: () => import('@/views/AdminDashboard.vue'),
        meta: { title: '管理后台', requiresAuth: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/AdminUsers.vue'),
        meta: { title: '用户管理', requiresAuth: true }
      },
      {
        path: 'admin/merchants',
        name: 'AdminMerchants',
        component: () => import('@/views/AdminMerchants.vue'),
        meta: { title: '商家审核', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.name === 'Login' && authStore.isAuthenticated) {
    next({ name: 'Products' })
  } else {
    next()
  }
})

export default router
