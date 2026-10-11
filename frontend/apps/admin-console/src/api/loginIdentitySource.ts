import request from '@/utils/request'
import { ENTERPRISE_SOURCE_TYPES, SOCIAL_SOURCE_TYPES } from '@/types/loginIdentitySource'
import type { LoginIdentitySource, LoginIdentitySourceStats, LoginIdentitySourceListResponse, LoginSourceType } from '@/types/loginIdentitySource'
import type {
  SocialIdentitySource,
  SocialIdentitySourceListResponse,
  CreateSocialIdentitySourceDto,
  UpdateSocialIdentitySourceDto,
} from '@/types/socialIdentitySource'
import type {
  EnterpriseIdentitySource,
  EnterpriseIdentitySourceListResponse,
  CreateEnterpriseIdentitySourceDto,
  UpdateEnterpriseIdentitySourceDto,
} from '@/types/enterpriseIdentitySource'

export interface LoginIdentitySourceQuery {
  type?: LoginSourceType
  status?: string
  search?: string
  page?: number
  pageSize?: number
}

/** 分类到身份源类型的映射：社会化与企业各自使用窄化后的类型。 */
interface LoginIdentitySourceCatalog {
  social: {
    source: SocialIdentitySource
    list: SocialIdentitySourceListResponse
    create: CreateSocialIdentitySourceDto
    update: UpdateSocialIdentitySourceDto
  }
  enterprise: {
    source: EnterpriseIdentitySource
    list: EnterpriseIdentitySourceListResponse
    create: CreateEnterpriseIdentitySourceDto
    update: UpdateEnterpriseIdentitySourceDto
  }
}

/** 跨分类汇总身份源时的只读列表能力，分类类型放宽到通用身份源。 */
export interface LoginIdentitySourceListApi {
  getList(query?: LoginIdentitySourceQuery): Promise<LoginIdentitySourceListResponse>
}

// 企业与社会化登录共用后端登录身份源协议，分类由厂商类型决定。
export function createLoginIdentitySourceApi<C extends keyof LoginIdentitySourceCatalog>(category: C) {
  type Source = LoginIdentitySourceCatalog[C]['source']
  type SourceList = LoginIdentitySourceCatalog[C]['list']
  type SourceCreate = LoginIdentitySourceCatalog[C]['create']
  type SourceUpdate = LoginIdentitySourceCatalog[C]['update']

  const types: readonly LoginSourceType[] = category === 'enterprise' ? ENTERPRISE_SOURCE_TYPES : SOCIAL_SOURCE_TYPES
  const categoryLabel = category === 'enterprise' ? '企业身份源' : '社会化身份源'

  function validateType(type: LoginSourceType) {
    if (!types.includes(type)) throw new Error(`该身份源类型不属于${categoryLabel}`)
  }

  function validateSource(source: LoginIdentitySource): Source {
    validateType(source.type)
    return source as Source
  }

  async function getById(id: string): Promise<Source> {
    return validateSource(await request.get(`/social-identity-sources/${id}`, { params: { category } }))
  }

  return {
    getById,
    getStats(): Promise<LoginIdentitySourceStats> {
      return request.get('/social-identity-sources/stats', { params: { category, type: category === 'enterprise' ? 'feishu_web' : undefined } })
    },
    async getList(query: LoginIdentitySourceQuery = {}): Promise<SourceList> {
      if (query.type) validateType(query.type)
      const result: LoginIdentitySourceListResponse = await request.get('/social-identity-sources', {
        params: { ...query, category, type: category === 'enterprise' ? 'feishu_web' : query.type },
      })
      result.items.forEach(validateSource)
      return result as SourceList
    },
    async create(input: SourceCreate): Promise<Source> {
      validateType(input.type)
      return validateSource(await request.post('/social-identity-sources', input, { params: { category } }))
    },
    async update(id: string, input: SourceUpdate): Promise<Source> {
      if (input.type) validateType(input.type)
      return validateSource(await request.put(`/social-identity-sources/${id}`, input, { params: { category } }))
    },
    async delete(id: string): Promise<void> {
      return request.delete(`/social-identity-sources/${id}`, { params: { category } })
    },
  }
}
