import request from '@/utils/request'
import { ENTERPRISE_SOURCE_TYPES, SOCIAL_SOURCE_TYPES } from '@/types/loginIdentitySource'
import type { LoginIdentitySource, LoginIdentitySourceStats, LoginIdentitySourceListResponse, LoginSourceType, CreateLoginIdentitySourceDto, UpdateLoginIdentitySourceDto } from '@/types/loginIdentitySource'

export interface LoginIdentitySourceQuery {
  type?: LoginSourceType
  status?: string
  search?: string
  page?: number
  pageSize?: number
}

// 企业与社会化登录共用后端登录身份源协议，分类由厂商类型决定。
export function createLoginIdentitySourceApi(category: 'social' | 'enterprise') {
  const types: readonly LoginSourceType[] = category === 'enterprise' ? ENTERPRISE_SOURCE_TYPES : SOCIAL_SOURCE_TYPES
  const categoryLabel = category === 'enterprise' ? '企业身份源' : '社会化身份源'

  function validateType(type: LoginSourceType) {
    if (!types.includes(type)) throw new Error(`该身份源类型不属于${categoryLabel}`)
  }

  function validateSource(source: LoginIdentitySource) {
    validateType(source.type)
    return source
  }

  async function getById(id: string): Promise<LoginIdentitySource> {
    return validateSource(await request.get(`/social-identity-sources/${id}`, { params: { category } }))
  }

  return {
    getById,
    getStats(): Promise<LoginIdentitySourceStats> {
      return request.get('/social-identity-sources/stats', { params: { category, type: category === 'enterprise' ? 'feishu_web' : undefined } })
    },
    async getList(query: LoginIdentitySourceQuery = {}): Promise<LoginIdentitySourceListResponse> {
      if (query.type) validateType(query.type)
      const result: LoginIdentitySourceListResponse = await request.get('/social-identity-sources', {
        params: { ...query, category, type: category === 'enterprise' ? 'feishu_web' : query.type },
      })
      result.items.forEach(validateSource)
      return result
    },
    async create(input: CreateLoginIdentitySourceDto): Promise<LoginIdentitySource> {
      validateType(input.type)
      return validateSource(await request.post('/social-identity-sources', input, { params: { category } }))
    },
    async update(id: string, input: UpdateLoginIdentitySourceDto): Promise<LoginIdentitySource> {
      if (input.type) validateType(input.type)
      return validateSource(await request.put(`/social-identity-sources/${id}`, input, { params: { category } }))
    },
    async delete(id: string): Promise<void> {
      return request.delete(`/social-identity-sources/${id}`, { params: { category } })
    },
  }
}
