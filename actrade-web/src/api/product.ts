import apiClient from './axios'

// ============ 通用响应类型 ============

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// ============ 商品分类相关 API ============

export type ProductCategoryStatus = 'ENABLED' | 'DISABLED'

export interface ProductCategory {
  categoryId: string
  parentId?: string
  name: string
  sortNo: number
  status: ProductCategoryStatus
  createdAt: string
  updatedAt: string
}

export interface CreateCategoryRequest {
  parentId?: string
  name: string
  sortNo?: number
  status?: ProductCategoryStatus
}

export const categoryApi = {
  list(params?: { status?: ProductCategoryStatus }) {
    return apiClient.get<ProductCategory[]>('/product-categories', { params })
  },

  getById(id: string) {
    return apiClient.get<ProductCategory>(`/product-categories/${id}`)
  },

  create(data: CreateCategoryRequest) {
    return apiClient.post<ProductCategory>('/product-categories', data)
  },

  update(id: string, data: Partial<CreateCategoryRequest>) {
    return apiClient.put<ProductCategory>(`/product-categories/${id}`, data)
  },

  delete(id: string) {
    return apiClient.delete(`/product-categories/${id}`)
  }
}

// ============ 商品相关 API ============

export type DeliveryMode = 'CODE_STOCK' | 'TOOL_EXECUTION' | 'MANUAL'
export type ProductStatus = 'DRAFT' | 'PENDING_REVIEW' | 'ONLINE' | 'OFFLINE' | 'ACTIVE' | 'INACTIVE'

export interface Product {
  productId: string
  merchantId: string
  categoryId: string
  name: string
  subtitle?: string
  description?: string
  coverImage?: string
  deliveryMode: DeliveryMode
  price: number
  originalPrice?: number
  status: ProductStatus
  stockCount: number
  salesCount: number
  createdAt: string
  updatedAt: string
}

export interface ProductDetail {
  productId: string
  description?: string
  images?: string[]
  parameters?: Record<string, string>
  createdAt: string
  updatedAt: string
}

export interface ProductListQuery {
  page?: number
  size?: number
  merchantId?: string
  categoryId?: string
  status?: ProductStatus
}

export interface CreateProductRequest {
  merchantId: string
  categoryId: string
  name: string
  subtitle?: string
  description?: string
  coverImage?: string
  deliveryMode: DeliveryMode
  price: number
  originalPrice?: number
  status?: ProductStatus
  stockCount?: number
  salesCount?: number
}

export interface UpdateProductRequest {
  categoryId?: string
  name?: string
  subtitle?: string
  description?: string
  coverImage?: string
  deliveryMode?: DeliveryMode
  price?: number
  originalPrice?: number
  status?: ProductStatus
  stockCount?: number
  salesCount?: number
}

export interface UpsertProductDetailRequest {
  description?: string
  images?: string[]
  parameters?: Record<string, string>
}

export const productApi = {
  list(params: ProductListQuery) {
    return apiClient.get<PageResponse<Product>>('/products', { params })
  },

  getById(id: string) {
    return apiClient.get<Product>(`/products/${id}`)
  },

  getDetail(id: string) {
    return apiClient.get<ProductDetail>(`/products/${id}/detail`)
  },

  create(data: CreateProductRequest) {
    return apiClient.post<Product>('/products', data)
  },

  update(id: string, data: UpdateProductRequest) {
    return apiClient.put<Product>(`/products/${id}`, data)
  },

  upsertDetail(id: string, data: UpsertProductDetailRequest) {
    return apiClient.put<ProductDetail>(`/products/${id}/detail`, data)
  },

  delete(id: string) {
    return apiClient.delete(`/products/${id}`)
  }
}

// ============ 订单相关 API ============

export type OrderStatus = 'CREATED' | 'PAID' | 'DELIVERING' | 'COMPLETED' | 'DELIVERY_FAILED' | 'CANCELED'
export type PaymentStatus = 'UNPAID' | 'PAYING' | 'PAID' | 'FAILED' | 'CLOSED' | 'REFUNDED'
export type PaymentMethod = 'MOCK' | 'MANUAL' | 'ALIPAY'
export type FulfillmentType = 'ACTIVATION_TOOL' | 'STOCK_CODE'
export type FulfillmentStatus = 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED'
export type SettlementStatus = 'UNSETTLED' | 'PENDING' | 'SETTLED'

export interface OrderItem {
  orderItemId: string
  productId: string
  productName: string
  deliveryMode: DeliveryMode
  unitPrice: number
  quantity: number
  subtotalAmount: number
  createdAt: string
}

export interface Order {
  orderId: string
  orderNo: string
  userId: string
  merchantId: string
  sellerId?: string

  totalAmount: number
  payAmount: number
  platformIncomeAmount?: number
  sellerIncomeAmount?: number

  orderStatus: OrderStatus
  paymentStatus: PaymentStatus
  paymentMethod?: PaymentMethod

  fulfillmentType?: FulfillmentType
  fulfillmentStatus?: FulfillmentStatus
  settlementStatus?: SettlementStatus

  paidAt?: string
  payDeadlineAt?: string
  deliveredAt?: string
  confirmDeadlineAt?: string
  closedAt?: string

  remark?: string
  createdAt: string
  updatedAt: string
  items?: OrderItem[]
}

export interface OrderSummary {
  orderId: string
  orderNo: string
  userId: string
  merchantId: string
  sellerId?: string

  totalAmount: number
  payAmount: number
  platformIncomeAmount?: number
  sellerIncomeAmount?: number

  orderStatus: OrderStatus
  paymentStatus: PaymentStatus
  paymentMethod?: PaymentMethod

  fulfillmentType?: FulfillmentType
  fulfillmentStatus?: FulfillmentStatus
  settlementStatus?: SettlementStatus

  paidAt?: string
  payDeadlineAt?: string
  deliveredAt?: string
  confirmDeadlineAt?: string
  closedAt?: string

  remark?: string
  createdAt: string
  updatedAt: string
}

export interface SubmitOrderItemRequest {
  productId: string
  quantity: number
}

export interface SubmitOrderRequest {
  userId: string
  items: SubmitOrderItemRequest[]
  remark?: string
}

export interface PaymentInitiateRequest {
  paymentMethod: PaymentMethod
}

export interface PaymentInitiateResponse {
  orderId: string
  orderNo: string
  paymentRequestNo?: string
  paymentMethod: PaymentMethod
  paymentStatus: PaymentStatus
  orderStatus: OrderStatus
  paid: boolean
  message?: string
  channelData?: {
    gateway?: string
    appId?: string
    outTradeNo?: string
    orderNo?: string
    amount?: string
    formHtml?: string
    qrCode?: string
    [key: string]: string | undefined
  }
}

export interface OrderListQuery {
  page?: number
  size?: number
  status?: OrderStatus
}

export const orderApi = {
  // 提交订单
  submitOrder(data: SubmitOrderRequest) {
    return apiClient.post<ApiResponse<Order>>('/orders', data)
  },

  // 获取订单详情
  getById(id: string) {
    return apiClient.get<ApiResponse<Order>>(`/orders/${id}`)
  },

  // 获取用户订单列表
  listUserOrders(userId: string, params?: OrderListQuery) {
    return apiClient.get<ApiResponse<OrderSummary[]>>('/orders', { params: { userId, ...params } })
  },

  // 获取用户订单列表（分页）
  pageUserOrders(userId: string, params?: OrderListQuery) {
    return apiClient.get<ApiResponse<PageResponse<OrderSummary>>>(`/orders/page`, { params: { userId, ...params } })
  },

  // 获取商家订单列表（分页）
  pageMerchantOrders(merchantId: string, params?: OrderListQuery) {
    return apiClient.get<ApiResponse<PageResponse<OrderSummary>>>(`/orders/merchant/${merchantId}/page`, { params })
  },

  // 取消订单
  cancelOrder(orderId: string) {
    return apiClient.post<ApiResponse<Order>>(`/orders/${orderId}/cancel`)
  },

  // 发起支付（MOCK/MANUAL兼容）
  payOrder(orderId: string, paymentMethod: PaymentMethod) {
    return apiClient.post<ApiResponse<Order>>(`/orders/${orderId}/pay`, { paymentMethod })
  },

  // 发起支付（新接口）
  initiatePayment(orderId: string, paymentMethod: PaymentMethod) {
    return apiClient.post<ApiResponse<PaymentInitiateResponse>>(`/orders/${orderId}/payment-initiate`, { paymentMethod })
  },

  // 手动发货
  deliverOrder(orderId: string) {
    return apiClient.post<ApiResponse<Order>>(`/orders/${orderId}/deliver`)
  },

  // 标记发货失败
  markDeliveryFailed(orderId: string) {
    return apiClient.post<ApiResponse<Order>>(`/orders/${orderId}/delivery-failed`)
  },

  // 确认收货
  confirmReceipt(orderId: string) {
    return apiClient.post<ApiResponse<Order>>(`/orders/${orderId}/confirm-receipt`)
  },

  // 手动准备结算
  prepareSettlement(orderId: string) {
    return apiClient.post<ApiResponse<Order>>(`/orders/${orderId}/settlement/prepare`)
  },

  // 手动完成结算
  markSettlementSettled(orderId: string) {
    return apiClient.post<ApiResponse<Order>>(`/orders/${orderId}/settlement/settle`)
  }
}

// ============ 商家结算相关 API ============

export interface SellerSettlement {
  settlementId: string
  settlementNo: string
  orderId: string
  sellerId: string
  merchantId: string
  settlementAmount: number
  settlementStatus: SettlementStatus
  settledAt?: string
  remark?: string
  createdAt: string
  updatedAt: string
}

export const settlementApi = {
  getById(settlementId: string) {
    return apiClient.get<ApiResponse<SellerSettlement>>(`/seller-settlements/${settlementId}`)
  },

  getByOrderId(orderId: string) {
    return apiClient.get<ApiResponse<SellerSettlement>>(`/seller-settlements/by-order/${orderId}`)
  },

  pageSellerSettlements(sellerId: string, params?: { status?: SettlementStatus; page?: number; size?: number }) {
    return apiClient.get<ApiResponse<PageResponse<SellerSettlement>>>(`/seller-settlements/seller/${sellerId}/page`, { params })
  },

  pageMerchantSettlements(merchantId: string, params?: { status?: SettlementStatus; page?: number; size?: number }) {
    return apiClient.get<ApiResponse<PageResponse<SellerSettlement>>>(`/seller-settlements/merchant/${merchantId}/page`, { params })
  },

  createPendingSettlement(orderId: string) {
    return apiClient.post<ApiResponse<SellerSettlement>>(`/seller-settlements/orders/${orderId}/prepare`)
  },

  settleOrder(orderId: string) {
    return apiClient.post<ApiResponse<SellerSettlement>>(`/seller-settlements/orders/${orderId}/settle`)
  }
}
