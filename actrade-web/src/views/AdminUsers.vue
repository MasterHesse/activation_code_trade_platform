<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { userApi, type User, type UserRole, type UserStatus } from '@/api/user'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const users = ref<User[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const roleFilter = ref<UserRole | ''>('')
const statusFilter = ref<UserStatus | ''>('')

// 弹窗
const editDialogVisible = ref(false)
const editFormRef = ref()
const editingUser = ref<User | null>(null)

const roleOptions: { label: string; value: string }[] = [
  { label: '全部角色', value: '' },
  { label: '普通用户', value: 'ROLE_USER' },
  { label: '商家', value: 'ROLE_MERCHANT' },
  { label: '管理员', value: 'ROLE_ADMIN' },
  { label: '超级管理员', value: 'ROLE_SUPER_ADMIN' }
]

const statusOptions: { label: string; value: string }[] = [
  { label: '全部状态', value: '' },
  { label: '正常', value: 'NORMAL' },
  { label: '禁用', value: 'DISABLED' },
  { label: '已删除', value: 'DELETED' }
]

const roleMap: Record<string, { label: string; type: string }> = {
  'ROLE_USER': { label: '普通用户', type: '' },
  'ROLE_MERCHANT': { label: '商家', type: 'success' },
  'ROLE_ADMIN': { label: '管理员', type: 'warning' },
  'ROLE_SUPER_ADMIN': { label: '超级管理员', type: 'danger' }
}

const statusMap: Record<string, { label: string; type: string }> = {
  'NORMAL': { label: '正常', type: 'success' },
  'DISABLED': { label: '禁用', type: 'danger' },
  'DELETED': { label: '已删除', type: 'info' }
}

const editForm = ref({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  role: '' as UserRole,
  status: '' as UserStatus
})

const editRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const checkAdmin = () => {
  if (!authStore.isAdmin && !authStore.isMerchant) {
    ElMessage.error('无权限访问此页面')
    router.push('/')
  }
}

const loadUsers = async () => {
  loading.value = true
  try {
    const response = await userApi.page({
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      role: roleFilter.value || undefined,
      status: statusFilter.value || undefined
    })

    users.value = response.data?.data?.content || []
    total.value = response.data?.data?.totalElements || 0
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  currentPage.value = 1
  loadUsers()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadUsers()
}

const handleEdit = (user: User) => {
  editingUser.value = user
  editForm.value = {
    username: user.username,
    nickname: user.nickname || '',
    email: user.email || '',
    phone: user.phone || '',
    role: user.role,
    status: user.status
  }
  editDialogVisible.value = true
}

const handleSaveEdit = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid: boolean) => {
    if (!valid || !editingUser.value) return

    try {
      await userApi.update(editingUser.value.userId, {
        nickname: editForm.value.nickname || undefined,
        email: editForm.value.email || undefined,
        phone: editForm.value.phone || undefined,
        role: editForm.value.role,
        status: editForm.value.status
      })

      ElMessage.success('用户信息已更新')
      editDialogVisible.value = false
      loadUsers()
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      ElMessage.error(err.response?.data?.message || '更新失败')
    }
  })
}

const handleDelete = async (user: User) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户「${user.username}」吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )

    await userApi.delete(user.userId)
    ElMessage.success('用户已删除')
    loadUsers()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    if (err.response?.data?.message) {
      ElMessage.error(err.response.data.message)
    } else if (err.message !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  checkAdmin()
  loadUsers()
})
</script>

<template>
  <div class="admin-users-page">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-input
            v-model="keyword"
            placeholder="搜索用户名/邮箱"
            clearable
            @keyup.enter="handleFilter"
          />
        </el-col>
        <el-col :span="6">
          <el-select v-model="roleFilter" placeholder="角色" clearable @change="handleFilter">
            <el-option
              v-for="opt in roleOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="statusFilter" placeholder="状态" clearable @change="handleFilter">
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleFilter">搜索</el-button>
          <el-button @click="loadUsers">刷新</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 用户列表 -->
    <el-card v-loading="loading" class="users-card">
      <el-table :data="users" stripe>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="roleMap[row.role]?.type || ''" size="small">
              {{ roleMap[row.role]?.label || row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || ''" size="small">
              {{ statusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最后登录" width="160">
          <template #default="{ row }">
            {{ row.lastLoginAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

      <el-empty v-if="!loading && users.length === 0" description="暂无用户" />
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="500px">
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-position="top"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="editForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="opt in roleOptions.filter(o => o.value)"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="editForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option
              v-for="opt in statusOptions.filter(o => o.value)"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.admin-users-page {
  padding: 24px;

  .page-header {
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

  .users-card {
    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: center;
    }
  }
}
</style>
