import type { LoginIdentitySource, LoginIdentitySourceListResponse, CreateLoginIdentitySourceDto, UpdateLoginIdentitySourceDto } from './loginIdentitySource'
import { ENTERPRISE_SOURCE_TYPES } from './loginIdentitySource'

export { ENTERPRISE_SOURCE_TYPES } from './loginIdentitySource'
export type EnterpriseSourceType = (typeof ENTERPRISE_SOURCE_TYPES)[number]
export type EnterpriseIdentitySource = Omit<LoginIdentitySource, 'type'> & { type: EnterpriseSourceType }
export type EnterpriseIdentitySourceListResponse = Omit<LoginIdentitySourceListResponse, 'items'> & { items: EnterpriseIdentitySource[] }
export type CreateEnterpriseIdentitySourceDto = Omit<CreateLoginIdentitySourceDto, 'type'> & { type: EnterpriseSourceType }
export type UpdateEnterpriseIdentitySourceDto = Omit<UpdateLoginIdentitySourceDto, 'type'> & { type?: EnterpriseSourceType }
export type { LoginIdentitySourceStats as EnterpriseIdentitySourceStats } from './loginIdentitySource'
