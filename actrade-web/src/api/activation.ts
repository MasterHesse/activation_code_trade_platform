import apiClient from './axios'
import type { PageResponse } from './product'

// ============ 激活码相关类型 ============

export type ActivationCodeStatus = 'AVAILABLE' | 'LOCKED' | 'SOLD' | 'VOID'

export interface ActivationCode {
  codeId: string
  productId: string
  merchantId: string
  batchNo: string
  codeValueEncrypted: string
  codeValueMasked: string
  codeValueHash: string
  status: ActivationCodeStatus
  assignedOrderId?: string
  assignedOrderItemId?: string
  expiredAt?: string
  remark?: string
  createdAt: string
  updatedAt: string
}

export interface CreateActivationCodeRequest {
  productId: string
  merchantId: string
  batchNo: string
  codeValueEncrypted: string
  codeValueMasked: string
  codeValueHash: string
  expiredAt?: string
  remark?: string
}

export interface BatchCreateActivationCodeItem {
  batchNo: string
  codeValueEncrypted: string
  codeValueMasked: string
  codeValueHash: string
  expiredAt?: string
  remark?: string
}

export interface BatchCreateActivationCodeRequest {
  productId: string
  merchantId: string
  items: BatchCreateActivationCodeItem[]
}

export interface UpdateActivationCodeStatusRequest {
  status: ActivationCodeStatus
}

export interface ActivationCodeQuery {
  productId?: string
  status?: ActivationCodeStatus
  page?: number
  size?: number
}

// ============ 激活工具相关类型 ============

export type ActivationToolStatus = 'ENABLED' | 'DISABLED'
export type ActivationToolAuditStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface ActivationTool {
  toolId: string
  merchantId: string
  productId: string
  toolName: string
  currentVersionId?: string
  toolStatus: ActivationToolStatus
  auditStatus: ActivationToolAuditStatus
  createdAt: string
  updatedAt: string
}

export interface CreateActivationToolRequest {
  merchantId: string
  productId: string
  toolName: string
}

export interface UpdateActivationToolNameRequest {
  toolName: string
}

export interface UpdateActivationToolStatusRequest {
  toolStatus: ActivationToolStatus
}

export interface UpdateActivationToolAuditStatusRequest {
  auditStatus: ActivationToolAuditStatus
  auditRemark?: string
}

export interface SetCurrentVersionRequest {
  toolVersionId: string
}

export interface ActivationToolQuery {
  merchantId?: string
  page?: number
  size?: number
}

// ============ 激活工具版本相关类型 ============

export type ActivationToolVersionStatus = 'DRAFT' | 'PENDING' | 'ENABLED' | 'DISABLED' | 'REJECTED'
export type FileScanStatus = 'PENDING' | 'SAFE' | 'DANGEROUS' | 'FAILED'
export type RuntimeType = 'JAR' | 'SHELL' | 'NATIVE_BINARY'
export type RuntimeOs = 'LINUX' | 'WINDOWS'
export type RuntimeArch = 'AMD64' | 'ARM64'

export interface ActivationToolVersion {
  toolVersionId: string
  toolId: string
  versionName: string
  fileId: string
  manifestContent?: Record<string, unknown>
  runtimeType: RuntimeType
  runtimeOs: RuntimeOs
  runtimeArch: RuntimeArch
  entrypoint: string
  execCommand: string
  timeoutSeconds: number
  maxMemoryMb: number
  fileSizeBytes?: number
  checksumSha256?: string
  scanStatus: FileScanStatus
  scanReport?: string
  reviewRemark?: string
  status: ActivationToolVersionStatus
  createdAt: string
  updatedAt: string
}

export interface CreateActivationToolVersionRequest {
  toolId: string
  versionName: string
  fileId: string
  manifestContent: Record<string, unknown>
  runtimeType: RuntimeType
  runtimeOs: RuntimeOs
  runtimeArch: RuntimeArch
  entrypoint: string
  execCommand: string
  timeoutSeconds: number
  maxMemoryMb: number
}

export interface UpdateActivationToolVersionStatusRequest {
  status: ActivationToolVersionStatus
}

export interface UpdateActivationToolVersionReviewRequest {
  reviewRemark?: string
  status: ActivationToolVersionStatus
}

export interface UpdateActivationToolVersionScanRequest {
  scanStatus: FileScanStatus
  scanReport?: string
}

export interface UpdateActivationToolVersionManifestRequest {
  manifestContent: Record<string, unknown>
}

export interface ActivationToolVersionQuery {
  toolId?: string
  page?: number
  size?: number
}

// ============ 激活任务相关类型 ============

export type ActivationTaskStatus = 'PENDING' | 'DISPATCHED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED'
export type ActivationTaskSourceType = 'ORDER_FULFILLMENT' | 'MANUAL' | 'RETRY' | 'SYSTEM'

export interface ActivationTask {
  id: number
  taskNo: string
  orderId: number
  orderItemId: number
  merchantId: number
  activationToolId: string
  activationToolVersionId: string
  status: ActivationTaskStatus
  sourceType: ActivationTaskSourceType
  maxAttempts: number
  attemptCount: number
  lastAttemptId?: number
  payloadJson?: string
  resultSummaryJson?: string
  errorCode?: string
  errorMessage?: string
  scheduledAt?: string
  startedAt?: string
  finishedAt?: string
  createdAt: string
  updatedAt: string
}

export interface CreateActivationTaskRequest {
  orderId?: number
  orderItemId?: number
  merchantId?: number
  activationToolId: string
  activationToolVersionId: string
  sourceType?: ActivationTaskSourceType
  maxAttempts?: number
  payloadJson: string
  scheduledAt?: string
}

// ============ 文件资源相关类型 ============

export type ActivationTaskArtifactType = 'STDOUT' | 'STDERR' | 'RESULT_FILE' | 'ERROR_LOG'

export interface FileAsset {
  fileId: string
  originalFilename: string
  storageProvider: string
  storagePath: string
  fileSizeBytes: number
  checksumSha256: string
  scanStatus: FileScanStatus
  scanReport?: string
  uploadedBy?: string
  createdAt: string
  updatedAt: string
}

export interface CreateFileAssetRequest {
  originalFilename: string
  storageProvider?: string
  storagePath: string
  fileSizeBytes: number
  checksumSha256: string
  uploadedBy?: string
}

export interface UpdateFileAssetScanRequest {
  scanStatus: FileScanStatus
  scanReport?: string
}

// ============ API 导出 ============

export const activationCodeApi = {
  create(data: CreateActivationCodeRequest) {
    return apiClient.post<ActivationCode>('/admin/activation-codes', data)
  },

  batchCreate(data: BatchCreateActivationCodeRequest) {
    return apiClient.post<ActivationCode[]>('/admin/activation-codes/batch', data)
  },

  getById(codeId: string) {
    return apiClient.get<ActivationCode>(`/admin/activation-codes/${codeId}`)
  },

  page(params: ActivationCodeQuery) {
    return apiClient.get<PageResponse<ActivationCode>>('/admin/activation-codes', { params })
  },

  findByBatchNo(batchNo: string) {
    return apiClient.get<ActivationCode[]>(`/admin/activation-codes/batch/${batchNo}`)
  },

  updateStatus(codeId: string, data: UpdateActivationCodeStatusRequest) {
    return apiClient.put<ActivationCode>(`/admin/activation-codes/${codeId}/status`, data)
  },

  voidCode(codeId: string) {
    return apiClient.put<ActivationCode>(`/admin/activation-codes/${codeId}/void`)
  },

  delete(codeId: string) {
    return apiClient.delete(`/admin/activation-codes/${codeId}`)
  }
}

export const activationToolApi = {
  create(data: CreateActivationToolRequest) {
    return apiClient.post<ActivationTool>('/admin/activation-tools', data)
  },

  getById(toolId: string) {
    return apiClient.get<ActivationTool>(`/admin/activation-tools/${toolId}`)
  },

  page(params: ActivationToolQuery) {
    return apiClient.get<PageResponse<ActivationTool>>('/admin/activation-tools', { params })
  },

  updateName(toolId: string, data: UpdateActivationToolNameRequest) {
    return apiClient.put<ActivationTool>(`/admin/activation-tools/${toolId}/name`, data)
  },

  updateStatus(toolId: string, data: UpdateActivationToolStatusRequest) {
    return apiClient.put<ActivationTool>(`/admin/activation-tools/${toolId}/status`, data)
  },

  updateAuditStatus(toolId: string, data: UpdateActivationToolAuditStatusRequest) {
    return apiClient.put<ActivationTool>(`/admin/activation-tools/${toolId}/audit-status`, data)
  },

  setCurrentVersion(toolId: string, data: SetCurrentVersionRequest) {
    return apiClient.put<ActivationTool>(`/admin/activation-tools/${toolId}/current-version`, data)
  },

  clearCurrentVersion(toolId: string) {
    return apiClient.delete<ActivationTool>(`/admin/activation-tools/${toolId}/current-version`)
  },

  delete(toolId: string) {
    return apiClient.delete(`/admin/activation-tools/${toolId}`)
  }
}

export const activationToolVersionApi = {
  create(data: CreateActivationToolVersionRequest) {
    return apiClient.post<ActivationToolVersion>('/admin/activation-tool-versions', data)
  },

  getById(toolVersionId: string) {
    return apiClient.get<ActivationToolVersion>(`/admin/activation-tool-versions/${toolVersionId}`)
  },

  page(params: ActivationToolVersionQuery) {
    return apiClient.get<PageResponse<ActivationToolVersion>>('/admin/activation-tool-versions', { params })
  },

  updateStatus(toolVersionId: string, data: UpdateActivationToolVersionStatusRequest) {
    return apiClient.put<ActivationToolVersion>(`/admin/activation-tool-versions/${toolVersionId}/status`, data)
  },

  updateReview(toolVersionId: string, data: UpdateActivationToolVersionReviewRequest) {
    return apiClient.put<ActivationToolVersion>(`/admin/activation-tool-versions/${toolVersionId}/review`, data)
  },

  updateScan(toolVersionId: string, data: UpdateActivationToolVersionScanRequest) {
    return apiClient.put<ActivationToolVersion>(`/admin/activation-tool-versions/${toolVersionId}/scan`, data)
  },

  updateManifest(toolVersionId: string, data: UpdateActivationToolVersionManifestRequest) {
    return apiClient.put<ActivationToolVersion>(`/admin/activation-tool-versions/${toolVersionId}/manifest`, data)
  },

  delete(toolVersionId: string) {
    return apiClient.delete(`/admin/activation-tool-versions/${toolVersionId}`)
  }
}

export const activationTaskApi = {
  create(data: CreateActivationTaskRequest) {
    return apiClient.post<ActivationTask>('/activation/tasks', data)
  },

  getById(id: number) {
    return apiClient.get<ActivationTask>(`/activation/tasks/${id}`)
  }
}

export const fileAssetApi = {
  create(data: CreateFileAssetRequest) {
    return apiClient.post<FileAsset>('/admin/file-assets', data)
  },

  getById(fileId: string) {
    return apiClient.get<FileAsset>(`/admin/file-assets/${fileId}`)
  },

  page(params?: { page?: number; size?: number }) {
    return apiClient.get<PageResponse<FileAsset>>('/admin/file-assets', { params })
  },

  getByChecksum(checksumSha256: string) {
    return apiClient.get<FileAsset>(`/admin/file-assets/checksum/${checksumSha256}`)
  },

  updateScan(fileId: string, data: UpdateFileAssetScanRequest) {
    return apiClient.put<FileAsset>(`/admin/file-assets/${fileId}/scan`, data)
  },

  delete(fileId: string) {
    return apiClient.delete(`/admin/file-assets/${fileId}`)
  }
}
