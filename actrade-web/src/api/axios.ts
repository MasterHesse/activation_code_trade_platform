import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

// 扩展 AxiosRequestConfig 以支持 suppressErrorMessage 选项
declare module 'axios' {
  export interface AxiosRequestConfig {
    suppressErrorMessage?: boolean
  }
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// 创建 axios 实例
const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore()
    
    if (authStore.accessToken) {
      config.headers.Authorization = `Bearer ${authStore.accessToken}`
    }
    
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    return response.data
  },
  async (error: AxiosError<{ message?: string }>) => {
    const authStore = useAuthStore()
    const originalRequest = error.config as InternalAxiosRequestConfig & { suppressErrorMessage?: boolean }
    
    // 处理 401 未授权
    if (error.response?.status === 401 && originalRequest && authStore.refreshToken) {
      try {
        // 尝试刷新 token
        const response = await axios.post(`${BASE_URL}/auth/refresh`, {
          refreshToken: authStore.refreshToken
        })
        
        if (response.data.accessToken) {
          authStore.setTokens(
            response.data.accessToken,
            response.data.refreshToken || authStore.refreshToken,
            response.data.expiresIn
          )
          
          // 重试原请求
          originalRequest.headers.Authorization = `Bearer ${response.data.accessToken}`
          return apiClient(originalRequest)
        }
      } catch (refreshError) {
        // 刷新失败，清除认证信息
        authStore.clearAuth()
        router.push({ name: 'Login' })
        ElMessage.error('登录已过期，请重新登录')
      }
    }
    
    // 显示错误消息（除非被标记为静默处理）
    if (!originalRequest?.suppressErrorMessage) {
      const errorMessage = error.response?.data?.message || error.message || '请求失败'
      if (error.response?.status !== 401) {
        ElMessage.error(errorMessage)
      }
    }
    
    return Promise.reject(error)
  }
)

export default apiClient
