import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import Cookies from 'js-cookie'

const ACCESS_TOKEN_KEY = 'actrade_access_token'
const REFRESH_TOKEN_KEY = 'actrade_refresh_token'
const USER_INFO_KEY = 'actrade_user_info'

export interface UserInfo {
  userId: string
  username: string
  role: string
}

// 从 localStorage 恢复 userInfo
function getStoredUserInfo(): UserInfo | null {
  try {
    const stored = localStorage.getItem(USER_INFO_KEY)
    return stored ? JSON.parse(stored) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(Cookies.get(ACCESS_TOKEN_KEY) || null)
  const refreshToken = ref<string | null>(Cookies.get(REFRESH_TOKEN_KEY) || null)
  const userInfo = ref<UserInfo | null>(getStoredUserInfo())
  const tokenExpiresIn = ref<number>(0)

  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => !!userInfo.value && userInfo.value.role === 'ROLE_ADMIN')
  const isMerchant = computed(() => !!userInfo.value && userInfo.value.role === 'ROLE_MERCHANT')
  const isUser = computed(() => !!userInfo.value && userInfo.value.role === 'ROLE_USER')

  function setTokens(access: string, refresh: string, expiresIn: number) {
    accessToken.value = access
    refreshToken.value = refresh
    tokenExpiresIn.value = expiresIn
    
    // 存储到 Cookie
    Cookies.set(ACCESS_TOKEN_KEY, access, { expires: expiresIn / 86400000 })
    Cookies.set(REFRESH_TOKEN_KEY, refresh, { expires: 7 })
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    // 持久化到 localStorage
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
  }

  function clearAuth() {
    accessToken.value = null
    refreshToken.value = null
    userInfo.value = null
    tokenExpiresIn.value = 0
    
    Cookies.remove(ACCESS_TOKEN_KEY)
    Cookies.remove(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    tokenExpiresIn,
    isAuthenticated,
    isAdmin,
    isMerchant,
    isUser,
    setTokens,
    setUserInfo,
    clearAuth
  }
})
