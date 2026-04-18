<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import {
  productApi, categoryApi,
  type Product, type ProductCategory, type ProductCategoryStatus, type ProductStatus,
  type CreateProductRequest, type CreateCategoryRequest
} from '@/api/product'
import { merchantApi, type Merchant } from '@/api/merchant'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const categories = ref<ProductCategory[]>([])
const products = ref<Product[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 商家状态
const merchantId = ref<string | null>(null)

// 弹窗
const categoryDialogVisible = ref(false)
const categoryEditDialogVisible = ref(false)
const productDialogVisible = ref(false)
const categoryFormRef = ref()
const categoryEditFormRef = ref()
const productFormRef = ref()

// 分类表单数据 - 新增
const categoryForm = ref<CreateCategoryRequest>({
  name: '',
  parentId: undefined,
  sortNo: 0,
  status: 'ENABLED'
})

// 分类表单数据 - 编辑
const categoryEditForm = ref({
  categoryId: '',
  name: '',
  sortNo: 0,
  status: 'ENABLED' as ProductCategoryStatus
})

// 当前编辑的分类
const editingCategory = ref<ProductCategory | null>(null)

const productForm = ref<CreateProductRequest>({
  merchantId: '',
  categoryId: '',
  name: '',
  subtitle: '',
  description: '',
  coverImage: '',
  deliveryMode: 'CODE_STOCK' as const,
  price: 0,
  originalPrice: undefined,
  status: 'DRAFT' as const,
  stockCount: 0
})

// 状态选项（使用后端期望的值：ONLINE/OFFLINE）
const statusOptions: Record<string, string> = {
  'DRAFT': '草稿',
  'PENDING_REVIEW': '待审核',
  'ONLINE': '上架',
  'OFFLINE': '下架',
  'DELETED': '已删除'
}

const statusTagType: Record<string, string> = {
  'DRAFT': 'info',
  'PENDING_REVIEW': 'warning',
  'ONLINE': 'success',
  'OFFLINE': '',
  'DELETED': 'danger'
}

const deliveryModeOptions = [
  { label: '激活码发货', value: 'CODE_STOCK' },
  { label: '工具执行', value: 'TOOL_EXECUTION' }
]

const categoryRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 20, message: '分类名称2-20个字符', trigger: 'blur' }
  ]
}

const productRules = {
  categoryId: [
    { required: true, message: '请选择商品分类', trigger: 'change' }
  ],
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 100, message: '商品名称2-100个字符', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '价格必须大于0', trigger: 'blur' }
  ],
  stockCount: [
    { required: true, message: '请输入库存', trigger: 'blur' },
    { type: 'number', min: 0, message: '库存不能为负数', trigger: 'blur' }
  ],
  deliveryMode: [
    { required: true, message: '请选择发货方式', trigger: 'change' }
  ]
}

// 确保用户已登录并获取最新用户信息
const ensureAuthenticated = async (): Promise<string | null> => {
  try {
    const authResponse = await authApi.getCurrentUser() as { userId?: string; username?: string; role?: string }
    if (authResponse?.userId) {
      authStore.setUserInfo({
        userId: authResponse.userId,
        username: authResponse.username || authResponse.userId,
        role: authResponse.role || 'ROLE_USER'
      })
      return authResponse.userId
    }
  } catch {
    ElMessage.error('获取用户信息失败，请重新登录')
    router.push('/login')
  }
  return null
}

// 加载商家信息
const loadMerchant = async () => {
  const userId = await ensureAuthenticated()
  if (!userId) return

  try {
    // 后端返回结构为 { code: 200, data: Merchant } 或 { code: 404, ... }
    const response = await merchantApi.getByUserId(userId) as unknown as { code?: number; data?: Merchant }
    if (response?.code === 200 && response.data) {
      const merchantData = response.data
      if (merchantData.auditStatus === 'APPROVED' && merchantData.merchantId) {
        merchantId.value = merchantData.merchantId
        productForm.value.merchantId = merchantData.merchantId
      } else {
        ElMessage.warning('您的商家资质尚未审核通过')
        router.push('/merchant/apply')
      }
    } else {
      ElMessage.warning('您还未申请成为商家')
      router.push('/merchant/apply')
    }
  } catch (e) {
    ElMessage.warning('您还未申请商家，请先申请')
    router.push('/merchant/apply')
  }
}

// 加载分类
const loadCategories = async () => {
  try {
    const response = await categoryApi.list() as unknown as ProductCategory[]
    categories.value = response || []
  } catch (e) {
    ElMessage.error('加载分类失败')
  }
}

// 加载商品列表
const loadProducts = async () => {
  if (!merchantId.value) return

  loading.value = true
  try {
    // 后端返回 Page<Product>，包含 content, totalElements 等字段
    const response = await productApi.list({
      merchantId: merchantId.value,
      page: currentPage.value - 1,
      size: pageSize.value
    }) as unknown as { content?: Product[]; totalElements?: number }

    // 确保正确解析响应数据
    if (Array.isArray(response?.content)) {
      products.value = response.content
      total.value = response.totalElements || 0
    } else {
      // 尝试从嵌套结构中获取数据
      const data = (response as unknown as { data?: { content?: Product[]; totalElements?: number } })?.data
      if (Array.isArray(data?.content)) {
        products.value = data.content
        total.value = data.totalElements || 0
      } else {
        products.value = []
        total.value = 0
      }
    }
  } catch (e) {
    ElMessage.error('加载商品失败')
    products.value = []
  } finally {
    loading.value = false
  }
}

// 创建分类
const handleCreateCategory = async () => {
  if (!categoryFormRef.value) return
  
  await categoryFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    
    try {
      await categoryApi.create(categoryForm.value)
      ElMessage.success('分类创建成功')
      categoryDialogVisible.value = false
      categoryFormRef.value.resetFields()
      loadCategories()
    } catch (e: unknown) {
      const error = e as { response?: { data?: { message?: string } } }
      ElMessage.error(error.response?.data?.message || '创建失败')
    }
  })
}

// 编辑分类 - 打开编辑弹窗
const handleEditCategory = (category: ProductCategory) => {
  editingCategory.value = category
  categoryEditForm.value = {
    categoryId: category.categoryId,
    name: category.name,
    sortNo: category.sortNo,
    status: category.status
  }
  categoryEditDialogVisible.value = true
}

// 更新分类
const handleUpdateCategory = async () => {
  if (!categoryEditFormRef.value) return
  
  await categoryEditFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    
    try {
      await categoryApi.update(categoryEditForm.value.categoryId, {
        name: categoryEditForm.value.name,
        sortNo: categoryEditForm.value.sortNo,
        status: categoryEditForm.value.status
      })
      ElMessage.success('分类更新成功')
      categoryEditDialogVisible.value = false
      loadCategories()
    } catch (e: unknown) {
      const error = e as { response?: { data?: { message?: string } } }
      ElMessage.error(error.response?.data?.message || '更新失败')
    }
  })
}

// 删除分类
const handleDeleteCategory = async (category: ProductCategory) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类「${category.name}」吗？`,
      '删除确认',
      { type: 'warning' }
    )
    
    await categoryApi.delete(category.categoryId)
    ElMessage.success('删除成功')
    loadCategories()
  } catch (e) {
    // 用户取消
  }
}

// 创建商品
const handleCreateProduct = async () => {
  if (!productFormRef.value) return
  
  await productFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    
    try {
      await productApi.create(productForm.value)
      ElMessage.success({
        message: '商品创建成功！请先导入激活码库存，再点击「上架」发布商品。',
        duration: 5000
      })
      productDialogVisible.value = false
      productFormRef.value.resetFields()
      productForm.value.merchantId = merchantId.value || ''
      loadProducts()
    } catch (e: unknown) {
      const error = e as { response?: { data?: { message?: string } } }
      ElMessage.error(error.response?.data?.message || '创建失败')
    }
  })
}

// 上下架商品（使用后端期望的状态值：ONLINE/OFFLINE）
const toggleProductStatus = async (product: Product) => {
  const newStatus: ProductStatus = product.status === 'ONLINE' ? 'OFFLINE' : 'ONLINE'
  const action = newStatus === 'ONLINE' ? '上架' : '下架'
  
  try {
    await productApi.update(product.productId, { status: newStatus })
    ElMessage.success(`商品${action}成功`)
    loadProducts()
  } catch (e) {
    ElMessage.error(`${action}失败`)
  }
}

// 删除商品
const handleDeleteProduct = async (product: Product) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除商品「${product.name}」吗？`,
      '删除确认',
      { type: 'warning' }
    )
    
    await productApi.delete(product.productId)
    ElMessage.success('删除成功')
    loadProducts()
  } catch (e) {
    // 用户取消
  }
}

// 分页
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadProducts()
}

onMounted(async () => {
  await loadMerchant()
  loadCategories()
  loadProducts()
})
</script>

<template>
  <div class="merchant-products-page">
    <div class="page-header">
      <h2 class="page-title">商品管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="categoryDialogVisible = true">
          新增分类
        </el-button>
        <el-button type="primary" @click="productDialogVisible = true">
          新增商品
        </el-button>
      </div>
    </div>
    
    <!-- 分类列表 -->
    <el-card class="category-card" header="商品分类">
      <div v-if="categories.length === 0" class="empty-categories">
        <el-empty description="暂无分类" />
      </div>
      <div class="category-list">
        <el-tag
          v-for="cat in categories"
          :key="cat.categoryId"
          :type="cat.status === 'ENABLED' ? 'success' : 'info'"
          class="category-tag"
          closable
          @close="handleDeleteCategory(cat)"
          @click="handleEditCategory(cat)"
        >
          {{ cat.name }}
          <span v-if="cat.status === 'DISABLED'" class="status-text">(已禁用)</span>
        </el-tag>
      </div>
    </el-card>
    
    <!-- 商品列表 -->
    <el-card class="product-card" header="商品列表">
      <div v-loading="loading">
        <el-table :data="products" stripe>
          <el-table-column prop="name" label="商品名称" min-width="150" />
          <el-table-column label="价格" width="120">
            <template #default="{ row }">
              ¥{{ row.price.toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column prop="stockCount" label="库存" width="100" />
          <el-table-column prop="salesCount" label="销量" width="100" />
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-tag :type="statusTagType[row.status] || ''">
                {{ statusOptions[row.status] || row.status }}
              </el-tag>
              <el-tooltip v-if="row.status === 'DRAFT'" content="导入库存后，点击「上架」发布商品" placement="top">
                <el-icon color="#E6A23C" style="margin-left: 4px; cursor: help;"><WarningFilled /></el-icon>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="发货方式" width="100">
            <template #default="{ row }">
              {{ deliveryModeOptions.find(m => m.value === row.deliveryMode)?.label || row.deliveryMode }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button 
                size="small" 
                :type="row.status === 'ONLINE' ? 'warning' : 'success'"
                @click="toggleProductStatus(row)"
              >
                {{ row.status === 'ONLINE' ? '下架' : '上架' }}
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                @click="handleDeleteProduct(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
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
    </el-card>
    
    <!-- 新增分类弹窗 -->
    <el-dialog v-model="categoryDialogVisible" title="新增分类" width="500px">
      <el-form
        ref="categoryFormRef"
        :model="categoryForm"
        :rules="categoryRules"
        label-position="top"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sortNo">
          <el-input-number v-model="categoryForm.sortNo" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="categoryForm.status">
            <el-radio-button value="ENABLED">启用</el-radio-button>
            <el-radio-button value="DISABLED">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateCategory">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 编辑分类弹窗 -->
    <el-dialog v-model="categoryEditDialogVisible" title="编辑分类" width="500px">
      <el-form
        ref="categoryEditFormRef"
        :model="categoryEditForm"
        :rules="categoryRules"
        label-position="top"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryEditForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sortNo">
          <el-input-number v-model="categoryEditForm.sortNo" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="categoryEditForm.status">
            <el-radio-button value="ENABLED">启用</el-radio-button>
            <el-radio-button value="DISABLED">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateCategory">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 新增商品弹窗 -->
    <el-dialog v-model="productDialogVisible" title="新增商品" width="600px">
      <el-form
        ref="productFormRef"
        :model="productForm"
        :rules="productRules"
        label-position="top"
      >
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="productForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat.categoryId"
              :label="cat.name"
              :value="cat.categoryId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="productForm.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="副标题" prop="subtitle">
          <el-input v-model="productForm.subtitle" placeholder="请输入副标题（选填）" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="productForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商品描述（选填）"
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品价格" prop="price">
              <el-input-number
                v-model="productForm.price"
                :min="0.01"
                :precision="2"
                :controls="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="划线价格" prop="originalPrice">
              <el-input-number
                v-model="productForm.originalPrice"
                :min="0"
                :precision="2"
                :controls="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="库存" prop="stockCount">
              <el-input-number
                v-model="productForm.stockCount"
                :min="0"
                :controls="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发货方式" prop="deliveryMode">
              <el-select v-model="productForm.deliveryMode" style="width: 100%">
                <el-option
                  v-for="opt in deliveryModeOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商品封面图" prop="coverImage">
          <el-input v-model="productForm.coverImage" placeholder="请输入封面图URL（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateProduct">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.merchant-products-page {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
  
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
  
  .category-card {
    margin-bottom: 24px;
    
    .empty-categories {
      padding: 20px 0;
    }
    
    .category-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    
    .category-tag {
      cursor: pointer;
      
      .status-text {
        font-size: 12px;
        margin-left: 4px;
      }
    }
  }
  
  .product-card {
    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: center;
    }
  }
}
</style>
