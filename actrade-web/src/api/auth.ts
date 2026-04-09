import apiClient from './axios'

// ============ 认证相关 API ============

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
  phone?: string
  nickname?: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userId: string
  username: string
  role: string
}

export const authApi = {
  login(data: LoginRequest) {
    return apiClient.post<AuthResponse>('/auth/login', data)
  },

  register(data: RegisterRequest) {
    return apiClient.post<AuthResponse>('/auth/register', data)
  },

  refreshToken(data: RefreshTokenRequest) {
    return apiClient.post<AuthResponse>('/auth/refresh', data)
  },

  getCurrentUser() {
    return apiClient.get<AuthResponse>('/auth/me')
  },

  logout() {
    return apiClient.post('/auth/logout')
  }
}
