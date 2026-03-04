export interface ApiResponse<T = any> {
  status: 'success' | 'error'
  message?: string
  data?: T
  error?: {
    code: string
    message: string
    details?: any
  }
}

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface ErrorResponse {
  status: 'error'
  message: string
  code?: string
  details?: any
}
