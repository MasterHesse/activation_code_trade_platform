import apiClient from './axios'
import type { ApiResponse, PageResponse } from './product'

// ============ 用户相关 API ============

export type UserRole = 'ROLE_USER' | 'ROLE_MERCHANT' | 'ROLE_ADMIN' | 'ROLE_SUPER_ADMIN'
export type UserStatus = 'NORMAL' | 'DISABLED' | 'DELETED'

export interface User {
  userId: string
  username: string
  nickname?: string
  email?: string
  phone?: string
  role: UserRole
  status: UserStatus
  lastLoginAt?: string
  createdAt: string
  updatedAt: string
}

export interface CreateUserRequest {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
  role?: UserRole
  status?: UserStatus
}

export interface UpdateUserRequest {
  username?: string
  password?: string
  nickname?: string
  email?: string
  phone?: string
  role?: UserRole
  status?: UserStatus
}

export interface UserListQuery {
  keyword?: string
  role?: UserRole
  status?: UserStatus
  page?: number
  size?: number
}

export const userApi = {
  // 创建用户（管理员）
  create(data: CreateUserRequest) {
    return apiClient.post<ApiResponse<User>>('/users', data)
  },

  // 获取用户详情
  getById(id: string) {
    return apiClient.get<ApiResponse<User>>(`/users/${id}`)
  },

  // 获取用户列表（分页）
  page(params: UserListQuery) {
    return apiClient.get<ApiResponse<PageResponse<User>>>(`/users`, { params })
  },

  // 更新用户
  update(id: string, data: UpdateUserRequest) {
    return apiClient.put<ApiResponse<User>>(`/users/${id}`, data)
  },

  // 删除用户
  delete(id: string) {
    return apiClient.delete<ApiResponse<void>>(`/users/${id}`)
  }
}
