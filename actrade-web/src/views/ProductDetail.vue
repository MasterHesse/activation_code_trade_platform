<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { productApi, orderApi, type Product, type ProductDetail, type SubmitOrderRequest } from '@/api/product'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
const product = ref<Product | null>(null)
const productDetail = ref<ProductDetail | null>(null)
const quantity = ref(1)

const productId = computed(() => route.params.id as string)

const isOnline = computed(() => product.value?.status === 'ONLINE')
const canBuy = computed(() => isOnline.value && (product.value?.stockCount ?? 0) > 0)

const deliveryModeText = computed(() => {
  const map: Record<string, string> = {
    'CODE_STOCK': '卡密发货',
    'TOOL_EXECUTION': '自动激活'
  }
  return product.value ? map[product.value.deliveryMode] || product.value.deliveryMode : ''
})

const statusText = computed(() => {
  const map: Record<string, string> = {
    'DRAFT': '草稿',
    'PENDING_REVIEW': '待审核',
    'ONLINE': '在售',
    'OFFLINE': '已下架'
  }
  return product.value ? map[product.value.status] || product.value.status : ''
})

const statusType = computed(() => {
  const map: Record<string, string> = {
    'DRAFT': 'info',
    'PENDING_REVIEW': 'warning',
    'ONLINE': 'success',
    'OFFLINE': ''
  }
  return product.value ? map[product.value.status] || '' : ''
})

const decrease = () => {
  if (quantity.value > 1) quantity.value--
}

const increase = () => {
  if (product.value && quantity.value < product.value.stockCount) {
    quantity.value++
  }
}

const handleBuy = async () => {
  if (!product.value || !authStore.userInfo?.userId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要购买 ${quantity.value} 件「${product.value.name}」吗？`,
      '确认购买',
      {
        confirmButtonText: '确定购买',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    submitting.value = true
    const request: SubmitOrderRequest = {
      userId: authStore.userInfo.userId,
      items: [{
        productId: product.value.productId,
        quantity: quantity.value
      }]
    }

    await orderApi.submitOrder(request)
    ElMessage.success('订单创建成功！')
    router.push('/orders')
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    if (err.response?.data?.message) {
      ElMessage.error(err.response.data.message)
    } else if (err.message !== 'cancel') {
      ElMessage.error('订单创建失败')
    }
  } finally {
    submitting.value = false
  }
}

const loadProduct = async () => {
  if (!productId.value) {
    ElMessage.error('商品ID无效')
    router.push('/products')
    return
  }

  loading.value = true
  try {
    const response = await productApi.getById(productId.value) as unknown as Product
    if (!response || !response.productId) {
      ElMessage.error('商品不存在或已下架')
      product.value = null
      return
    }
    product.value = response

    // 加载商品详情（可选，不影响主流程）
    try {
      const response = await productApi.getDetail(productId.value) as unknown as { data?: ProductDetail }
      // 响应结构为 { code: 200, message: "success", data: { productId, description, ... } }
      if (response?.data) {
        productDetail.value = response.data
      }
    } catch {
      // 商品详情可能不存在，忽略错误
    }
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string }
    const errorMsg = err.response?.data?.message || err.message || '加载商品详情失败'
    ElMessage.error(errorMsg)
    product.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProduct()
})
</script>

<template>
  <div class="product-detail-page">
    <div v-loading="loading" class="detail-container">
      <template v-if="product">
        <el-card>
          <el-row :gutter="24">
            <!-- 商品图片 -->
            <el-col :xs="24" :sm="10">
              <div class="product-image">
                <el-image
                  v-if="product.coverImage"
                  :src="product.coverImage"
                  :alt="product.name"
                  fit="contain"
                >
                  <template #error>
                    <div class="image-placeholder">
                      <el-icon :size="48"><Picture /></el-icon>
                      <span>暂无图片</span>
                    </div>
                  </template>
                </el-image>
                <div v-else class="image-placeholder">
                  <el-icon :size="48"><Picture /></el-icon>
                  <span>暂无图片</span>
                </div>
              </div>
            </el-col>

            <!-- 商品信息 -->
            <el-col :xs="24" :sm="14">
              <div class="product-info">
                <h1 class="product-title">{{ product.name }}</h1>

                <div v-if="product.subtitle" class="product-subtitle">
                  {{ product.subtitle }}
                </div>

                <div class="product-meta">
                  <el-tag :type="statusType">{{ statusText }}</el-tag>
                  <span class="delivery-mode">{{ deliveryModeText }}</span>
                </div>

                <div class="product-price-section">
                  <div class="price-row">
                    <span class="price-label">价格</span>
                    <span class="price">¥{{ product.price.toFixed(2) }}</span>
                    <span v-if="product.originalPrice && product.originalPrice > product.price" class="original-price">
                      ¥{{ product.originalPrice.toFixed(2) }}
                    </span>
                  </div>
                  <div class="sales-info">
                    <span>销量 {{ product.salesCount }}</span>
                    <span class="separator">|</span>
                    <span>库存 {{ product.stockCount }}</span>
                  </div>
                </div>

                <div class="product-quantity">
                  <span class="label">数量</span>
                  <div class="quantity-control">
                    <el-button :disabled="quantity <= 1" @click="decrease">-</el-button>
                    <el-input-number
                      v-model="quantity"
                      :min="1"
                      :max="product.stockCount"
                      :disabled="!canBuy"
                      controls-position="right"
                    />
                    <el-button :disabled="quantity >= product.stockCount" @click="increase">+</el-button>
                  </div>
                </div>

                <div class="product-actions">
                  <el-button
                    type="primary"
                    size="large"
                    :loading="submitting"
                    :disabled="!canBuy"
                    @click="handleBuy"
                  >
                    {{ canBuy ? '立即购买' : (isOnline ? '库存不足' : '暂不可购买') }}
                  </el-button>
                  <el-button size="large" @click="router.push('/products')">
                    返回列表
                  </el-button>
                </div>
              </div>
            </el-col>
          </el-row>

          <!-- 商品描述 -->
          <el-divider content-position="left">
            <span class="divider-title">商品详情</span>
          </el-divider>

          <div class="product-description">
            <div v-if="productDetail?.description || product.description" class="description-content">
              <div v-if="productDetail?.description" v-html="productDetail.description.replace(/\n/g, '<br>')"></div>
              <div v-else>{{ product.description || '暂无详细描述' }}</div>
            </div>
            <el-empty v-else description="暂无详细描述" :image-size="80" />
          </div>
        </el-card>
      </template>

      <el-empty v-else description="商品不存在">
        <el-button type="primary" @click="router.push('/products')">返回商品列表</el-button>
      </el-empty>
    </div>
  </div>
</template>

<style scoped lang="scss">
.product-detail-page {
  padding: 24px;

  .detail-container {
    max-width: 1200px;
    margin: 0 auto;
    min-height: 500px;
  }

  .product-image {
    background: #f5f7fa;
    border-radius: 8px;
    overflow: hidden;
    min-height: 400px;
    display: flex;
    align-items: center;
    justify-content: center;

    :deep(.el-image) {
      width: 100%;
      height: 400px;
    }

    .image-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #909399;
      gap: 8px;
    }
  }

  .product-info {
    padding: 0 16px;

    .product-title {
      font-size: 24px;
      margin: 0 0 8px;
      color: #303133;
      line-height: 1.4;
    }

    .product-subtitle {
      color: #909399;
      margin-bottom: 16px;
      font-size: 14px;
    }

    .product-meta {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-bottom: 20px;

      .delivery-mode {
        color: #606266;
        font-size: 14px;
      }
    }

    .product-price-section {
      background: linear-gradient(135deg, #fef0f0 0%, #fff7f0 100%);
      padding: 20px;
      border-radius: 8px;
      margin-bottom: 20px;

      .price-row {
        display: flex;
        align-items: baseline;
        gap: 12px;
        margin-bottom: 8px;
      }

      .price-label {
        color: #909399;
        font-size: 14px;
      }

      .price {
        font-size: 32px;
        font-weight: bold;
        color: #f56c6c;
      }

      .original-price {
        font-size: 16px;
        color: #c0c4cc;
        text-decoration: line-through;
      }

      .sales-info {
        color: #909399;
        font-size: 13px;

        .separator {
          margin: 0 8px;
        }
      }
    }

    .product-quantity {
      margin-bottom: 24px;

      .label {
        display: block;
        color: #606266;
        margin-bottom: 8px;
      }

      .quantity-control {
        display: flex;
        align-items: center;
        gap: 8px;

        :deep(.el-input-number) {
          width: 120px;
        }
      }
    }

    .product-actions {
      display: flex;
      gap: 12px;

      :deep(.el-button--large) {
        padding: 12px 32px;
      }
    }
  }

  .product-description {
    padding: 16px 0;

    .divider-title {
      font-size: 16px;
      font-weight: 600;
    }

    .description-content {
      color: #606266;
      line-height: 1.8;
      font-size: 14px;

      :deep(p) {
        margin: 0 0 12px;
      }
    }
  }
}
</style>
