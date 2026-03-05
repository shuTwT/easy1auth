import request from '@/utils/request'
import type {
  UserGroup,
  CreateGroupDto,
  UpdateGroupDto,
  GroupQueryDto,
  GroupListResponse,
  GroupTreeResponse,
  GroupMembersResponse,
  GroupStats,
  AddMembersDto,
  RemoveMembersDto,
  AddAdminsDto,
  RemoveAdminsDto
} from '@/types/group'

export const groupApi = {
  getList(query: GroupQueryDto): Promise<{ status: string; data: GroupListResponse }> {
    return request.get('/groups', { params: query })
  },

  getById(id: string): Promise<{ status: string; data: UserGroup }> {
    return request.get(`/groups/${id}`)
  },

  create(data: CreateGroupDto): Promise<{ status: string; message: string; data: UserGroup }> {
    return request.post('/groups', data)
  },

  update(id: string, data: UpdateGroupDto): Promise<{ status: string; message: string; data: UserGroup }> {
    return request.put(`/groups/${id}`, data)
  },

  delete(id: string): Promise<{ status: string; message: string }> {
    return request.delete(`/groups/${id}`)
  },

  getTree(): Promise<{ status: string; data: GroupTreeResponse[] }> {
    return request.get('/groups/tree')
  },

  getStats(): Promise<{ status: string; data: GroupStats }> {
    return request.get('/groups/stats')
  },

  getMembers(id: string): Promise<{ status: string; data: GroupMembersResponse }> {
    return request.get(`/groups/${id}/members`)
  },

  addMembers(id: string, data: AddMembersDto): Promise<{ status: string; message: string }> {
    return request.post(`/groups/${id}/members`, data)
  },

  removeMembers(id: string, data: RemoveMembersDto): Promise<{ status: string; message: string }> {
    return request.delete(`/groups/${id}/members`, { data })
  },

  addAdmins(id: string, data: AddAdminsDto): Promise<{ status: string; message: string }> {
    return request.post(`/groups/${id}/admins`, data)
  },

  removeAdmins(id: string, data: RemoveAdminsDto): Promise<{ status: string; message: string }> {
    return request.delete(`/groups/${id}/admins`, { data })
  }
}
