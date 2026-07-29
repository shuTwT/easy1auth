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
  getList(query: GroupQueryDto): Promise<GroupListResponse> {
    return request.get('/groups', { params: query })
  },

  getById(id: string): Promise<UserGroup> {
    return request.get(`/groups/${id}`)
  },

  create(data: CreateGroupDto): Promise<UserGroup> {
    return request.post('/groups', data)
  },

  update(id: string, data: UpdateGroupDto): Promise<UserGroup> {
    return request.put(`/groups/${id}`, data)
  },

  delete(id: string): Promise<void> {
    return request.delete(`/groups/${id}`)
  },

  getTree(): Promise<GroupTreeResponse[]> {
    return request.get('/groups/tree')
  },

  getStats(): Promise<GroupStats> {
    return request.get('/groups/stats')
  },

  getMembers(id: string): Promise<GroupMembersResponse> {
    return request.get(`/groups/${id}/members`)
  },

  addMembers(id: string, data: AddMembersDto): Promise<void> {
    return request.post(`/groups/${id}/members`, data)
  },

  removeMembers(id: string, data: RemoveMembersDto): Promise<void> {
    return request.delete(`/groups/${id}/members`, { data })
  },

  addAdmins(id: string, data: AddAdminsDto): Promise<void> {
    return request.post(`/groups/${id}/admins`, data)
  },

  removeAdmins(id: string, data: RemoveAdminsDto): Promise<void> {
    return request.delete(`/groups/${id}/admins`, { data })
  }
}
