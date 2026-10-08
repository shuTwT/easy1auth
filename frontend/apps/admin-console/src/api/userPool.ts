import request from '@/utils/request'
import type { UserPoolInfo } from '@/types/auth'
import type { UserPool, CreateUserPoolDto, UserPoolControl, UserPoolControlListResponse, UserPoolQueryDto, UserPoolListResponse, UserPoolUpdateDto } from '@/types/userPool'

export interface CreateUserPoolRequest {
  name: string
  packageId: number
}

export interface UserPoolPackageOption {
  id: number
  name: string
  maxUsers: number
  maxApps: number
}

export const userPoolApi = {
  getSimpleList(): Promise<UserPoolInfo[]> {
    return request.get('/user-pools/simple-list')
  },

  getCurrentSimpleList(): Promise<UserPoolInfo[]> {
    return request.get('/user-pools/simple-slist/current')
  },

  createUserPool(data: CreateUserPoolRequest): Promise<UserPoolInfo> {
    return request.post('/user-pools/create', data)
  },

  getCurrentUserPool(): Promise<UserPoolInfo> {
    return request.get('/user-pools/current')
  },

  getAvailablePackages(): Promise<UserPoolPackageOption[]> {
    return request.get('/user-pools/available-packages')
  },

  getList(params: UserPoolQueryDto): Promise<UserPoolListResponse> {
    return request.get('/user-pools/list', { params })
  },

  getManagedList(): Promise<UserPoolControlListResponse> {
    return request.get('/user-pools/managed')
  },

  create(data: CreateUserPoolDto): Promise<UserPool> {
    return request.post('/user-pools/create', data)
  },

  update(id: string, data: UserPoolUpdateDto): Promise<UserPoolControl> {
    return request.put(`/user-pools/${id}`, data)
  },

  delete(id: string): Promise<void> {
    return request.delete(`/user-pools/${id}`)
  },

  updateStatus(id: string, status: 'active' | 'suspended'): Promise<UserPoolControl> {
    return request.put(`/user-pools/${id}/status`, { status })
  },

  transferAdministrator(id: string, administratorAccountId: string): Promise<UserPoolControl> {
    return request.put(`/user-pools/${id}/administrator`, { administratorAccountId })
  }
}
