<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { productApi, categoryApi, type Product, type ProductCategory } from '@/api/product'

const router = useRouter()

const loading = ref(false)
const productList = ref<Product[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)

const searchKeyword = ref('')
const selectedCategory = ref('')
const categories = ref<string[]>([])

const filteredProducts = computed(() => {
  let result = productList.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(p =>
      p.name.toLowerCase().includes(keyword) ||
      (p.description && p.description.toLowerCase().includes(keyword))
    )
  }

  if (selectedCategory.value) {
    result = result.filter(p => p.categoryId === selectedCategory.value)
  }

  return result
})

const loadProducts = async () => {
  loading.value = true
  try {
    const response = await productApi.list({
      page: currentPage.value - 1,
      size: pageSize.value
    }) as unknown as { content?: Product[]; totalElements?: number }
    productList.value = response?.content || []
    total.value = response?.totalElements || 0
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const response = await categoryApi.list() as unknown as ProductCategory[]
    categories.value = response?.map(c => c.categoryId) || []
  } catch (e) {
    // 忽略
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadProducts()
}

const goToDetail = (product: Product) => {
  router.push(`/products/${product.productId}`)
}

onMounted(() => {
  loadProducts()
  loadCategories()
})
</script>

<template>
  <div class="products-page">
    <div class="page-header">
      <h2 class="page-title">商品列表</h2>
    </div>
    
    <!-- 搜索筛选 -->
    <div class="card-container filter-bar">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索商品名称或描述"
            prefix-icon="Search"
            clearable
          />
        </el-col>
        <el-col :span="6">
          <el-select
            v-model="selectedCategory"
            placeholder="选择分类"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="cat in categories"
              :key="cat"
              :label="cat"
              :value="cat"
            />
          </el-select>
        </el-col>
      </el-row>
    </div>
    
    <!-- 商品列表 -->
    <div v-loading="loading" class="product-grid">
      <el-empty v-if="filteredProducts.length === 0" description="暂无商品" />
      
      <el-row v-else :gutter="16">
        <el-col
          v-for="product in filteredProducts"
          :key="product.productId"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <el-card
            class="product-card"
            :body-style="{ padding: '0px' }"
            shadow="hover"
            @click="goToDetail(product)"
          >
            <div class="product-image">
              <img :src="product.coverImage || '/placeholder.png'" :alt="product.name" />
              <el-tag
                v-if="product.status === 'ACTIVE'"
                type="success"
                class="stock-tag"
              >
                库存: {{ product.stockCount }}
              </el-tag>
              <el-tag v-else type="info" class="stock-tag">
                {{ product.status === 'DRAFT' ? '草稿' : '已下架' }}
              </el-tag>
            </div>
            
            <div class="product-info">
              <h3 class="product-name">{{ product.name }}</h3>
              <p class="product-desc">{{ product.description }}</p>
              <div class="product-footer">
                <span class="product-price">¥{{ product.price.toFixed(2) }}</span>
                <el-button type="primary" size="small">立即购买</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    
    <!-- 分页 -->
    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.products-page {
  .filter-bar {
    margin-bottom: 16px;
  }
  
  .product-grid {
    min-height: 400px;
  }
  
  .product-card {
    margin-bottom: 16px;
    cursor: pointer;
    transition: transform 0.2s;
    
    &:hover {
      transform: translateY(-4px);
    }
    
    .product-image {
      position: relative;
      height: 160px;
      background: #f5f7fa;
      overflow: hidden;
      
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      .stock-tag {
        position: absolute;
        top: 8px;
        right: 8px;
      }
    }
    
    .product-info {
      padding: 12px;
      
      .product-name {
        margin: 0 0 8px;
        font-size: 16px;
        font-weight: 500;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      .product-desc {
        margin: 0 0 12px;
        font-size: 12px;
        color: #909399;
        height: 36px;
        overflow: hidden;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
      }
      
      .product-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .product-price {
          font-size: 20px;
          font-weight: bold;
          color: #f56c6c;
        }
      }
    }
  }
  
  .pagination {
    display: flex;
    justify-content: center;
    margin-top: 24px;
  }
}
</style>
