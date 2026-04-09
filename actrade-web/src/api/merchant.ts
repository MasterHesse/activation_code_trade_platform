import apiClient from './axios'

// ============ 商家相关 API ============

export type MerchantType = 'PERSONAL' | 'TEAM' | 'ENTERPRISE'
export type MerchantAuditStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface Merchant {
  merchantId: string
  userId: string
  merchantName: string
  merchantType: MerchantType
  contactName?: string
  contactEmail?: string
  contactPhone?: string
  licenseInfo?: string
  auditStatus: MerchantAuditStatus
  auditRemark?: string
  createdAt: string
  updatedAt: string
}

export interface CreateMerchantRequest {
  userId: string
  merchantName: string
  merchantType: MerchantType
  contactName?: string
  contactEmail?: string
  contactPhone?: string
  licenseInfo?: string
}

export interface UpdateMerchantRequest {
  merchantName?: string
  merchantType?: MerchantType
  contactName?: string
  contactEmail?: string
  contactPhone?: string
  licenseInfo?: string
  auditStatus?: MerchantAuditStatus
  auditRemark?: string
}

export interface AuditMerchantRequest {
  auditStatus: MerchantAuditStatus
  auditRemark?: string
}

export const merchantApi = {
  // 申请成为商家
  apply(data: CreateMerchantRequest) {
    return apiClient.post<Merchant>('/merchants', data)
  },

  // 获取商家列表
  list() {
    return apiClient.get<Merchant[]>('/merchants')
  },

  // 获取商家详情
  getById(id: string) {
    return apiClient.get<Merchant>(`/merchants/${id}`)
  },

  // 根据用户ID获取商家信息（预期可能返回404，静默处理错误消息）
  getByUserId(userId: string) {
    return apiClient.get<Merchant>(`/merchants/by-user/${userId}`, {
      suppressErrorMessage: true
    })
  },

  // 更新商家信息
  update(id: string, data: UpdateMerchantRequest) {
    return apiClient.put<Merchant>(`/merchants/${id}`, data)
  },

  // 审核商家（管理员专用）
  audit(id: string, data: AuditMerchantRequest) {
    return apiClient.put<Merchant>(`/merchants/${id}/audit`, data)
  },

  // 删除商家
  delete(id: string) {
    return apiClient.delete(`/merchants/${id}`)
  },

  // 检查用户是否为商家（预期可能返回404，静默处理错误消息）
  checkMerchantStatus(userId: string) {
    return apiClient.get<Merchant>(`/merchants/by-user/${userId}`, {
      suppressErrorMessage: true
    })
  }
}
