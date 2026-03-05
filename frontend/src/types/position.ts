export interface Position {
  id: string
  tenantId: string
  name: string
  code: string
  description?: string
  departmentId?: string
  level: number
  sequence?: string
  userCount: number
  maxCount?: number
  createdAt: string
  updatedAt: string
}

export interface CreatePositionDto {
  name: string
  code: string
  description?: string
  departmentId?: string
  level?: number
  sequence?: string
  maxCount?: number
}

export interface UpdatePositionDto {
  name?: string
  code?: string
  description?: string
  departmentId?: string
  level?: number
  sequence?: string
  maxCount?: number
}

export interface PositionQueryDto {
  page?: number
  pageSize?: number
  name?: string
  code?: string
  departmentId?: string
  level?: number
}

export interface PositionListResponse {
  positions: Position[]
  total: number
  page: number
  pageSize: number
}

export interface PositionStats {
  totalPositions: number
  filledPositions: number
  vacantPositions: number
  averageLevel: number
}
