import request from '@/utils/request'
import type { SyncIdentitySource, SyncIdentitySourceInput, SyncIdentityTask, SyncIdentitySourceStats, SyncIdentitySourceQuery, SyncIdentitySourceListResponse } from '@/types/syncIdentitySource'

// 通讯录同步沿用已有后端地址；该地址不用于企业登录身份源。
export const SYNC_IDENTITY_SOURCE_API_PATH = '/enterprise-identity-sources'

export const syncIdentitySourceApi = {
  getStats(): Promise<SyncIdentitySourceStats> {
    return request.get(`${SYNC_IDENTITY_SOURCE_API_PATH}/stats`)
  },
  getList(params: SyncIdentitySourceQuery): Promise<SyncIdentitySourceListResponse> {
    return request.get(SYNC_IDENTITY_SOURCE_API_PATH, { params })
  },
  create(input: SyncIdentitySourceInput): Promise<SyncIdentitySource> {
    return request.post(SYNC_IDENTITY_SOURCE_API_PATH, input)
  },
  update(id: string, input: SyncIdentitySourceInput): Promise<SyncIdentitySource> {
    return request.put(`${SYNC_IDENTITY_SOURCE_API_PATH}/${id}`, input)
  },
  delete(id: string): Promise<void> {
    return request.delete(`${SYNC_IDENTITY_SOURCE_API_PATH}/${id}`)
  },
  sync(id: string): Promise<SyncIdentityTask> {
    return request.post(`${SYNC_IDENTITY_SOURCE_API_PATH}/${id}/sync`)
  },
  tasks(id: string): Promise<SyncIdentityTask[]> {
    return request.get(`${SYNC_IDENTITY_SOURCE_API_PATH}/${id}/tasks`)
  },
}
