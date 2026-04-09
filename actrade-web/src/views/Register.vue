<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { authApi, type RegisterRequest, type AuthResponse } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const loading = ref(false)

// 注册类型: 'user' = 普通用户, 'merchant' = 商家
const registerType = ref<'user' | 'merchant'>('user')

const formData = ref<RegisterRequest & { confirmPassword: string }>({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  nickname: ''
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== formData.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 64, message: '用户名3-64个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 128, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    loading.value = true
    try {
      const requestData = {
        username: formData.value.username,
        password: formData.value.password,
        email: formData.value.email || undefined,
        phone: formData.value.phone || undefined,
        nickname: formData.value.nickname || undefined
      }

      const response = await authApi.register(requestData) as unknown as AuthResponse

      authStore.setTokens(response.accessToken, response.refreshToken, response.expiresIn)
      authStore.setUserInfo({
        userId: response.userId,
        username: response.username,
        role: response.role
      })

      ElMessage.success('注册成功！')

      // 如果选择注册为商家，跳转到商家申请页面
      if (registerType.value === 'merchant') {
        router.push('/merchant/apply')
      } else {
        router.push('/products')
      }
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string }
      ElMessage.error(err.response?.data?.message || err.message || '注册失败，请重试')
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-logo">
        <h1>注册账号</h1>
        <p>加入 ACTrade 激活码交易平台</p>
      </div>
      
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <!-- 注册类型选择 -->
        <div class="register-type-selector">
          <el-radio-group v-model="registerType" size="large">
            <el-radio-button value="user">
              <el-icon><User /></el-icon> 普通用户
            </el-radio-button>
            <el-radio-button value="merchant">
              <el-icon><Shop /></el-icon> 商家入驻
            </el-radio-button>
          </el-radio-group>
          <p v-if="registerType === 'merchant'" class="type-hint">
            入驻后可发布商品进行销售，享受平台服务
          </p>
        </div>
        
        <el-form-item prop="username">
          <el-input
            v-model="formData.username"
            placeholder="用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="nickname">
          <el-input
            v-model="formData.nickname"
            placeholder="昵称（选填）"
            size="large"
            prefix-icon="UserFilled"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="formData.email"
            placeholder="邮箱（选填）"
            size="large"
            prefix-icon="Message"
          />
        </el-form-item>

        <el-form-item prop="phone">
          <el-input
            v-model="formData.phone"
            placeholder="手机号（选填）"
            size="large"
            prefix-icon="Phone"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="formData.confirmPassword"
            type="password"
            placeholder="确认密码"
            size="large"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleSubmit"
          >
            {{ registerType === 'merchant' ? '注册并申请商家' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;

  .login-card {
    width: 100%;
    max-width: 400px;
    padding: 40px;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);

    .login-logo {
      text-align: center;
      margin-bottom: 32px;

      h1 {
        margin: 0 0 8px;
        font-size: 28px;
        font-weight: 600;
        color: #303133;
      }

      p {
        margin: 0;
        color: #909399;
        font-size: 14px;
      }
    }

    .register-type-selector {
      margin-bottom: 24px;
      text-align: center;
      
      .el-radio-group {
        width: 100%;
        
        :deep(.el-radio-button) {
          width: 50%;
          
          .el-radio-button__inner {
            width: 100%;
          }
        }
      }
      
      .type-hint {
        margin: 8px 0 0;
        font-size: 12px;
        color: #909399;
      }
    }

    .login-footer {
      margin-top: 24px;
      text-align: center;
      color: #909399;
      font-size: 14px;

      a {
        color: #409eff;
        text-decoration: none;
        margin-left: 4px;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }
}
</style>
