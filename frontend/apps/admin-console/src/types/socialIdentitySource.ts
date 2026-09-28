import type { LoginIdentitySource, LoginIdentitySourceListResponse, CreateLoginIdentitySourceDto, UpdateLoginIdentitySourceDto } from './loginIdentitySource'
import { SOCIAL_SOURCE_TYPES } from './loginIdentitySource'

export { SOCIAL_SOURCE_TYPES } from './loginIdentitySource'
export type SocialSourceType = (typeof SOCIAL_SOURCE_TYPES)[number]
export type SocialIdentitySource = Omit<LoginIdentitySource, 'type'> & { type: SocialSourceType }
export type SocialIdentitySourceListResponse = Omit<LoginIdentitySourceListResponse, 'items'> & { items: SocialIdentitySource[] }
export type CreateSocialIdentitySourceDto = Omit<CreateLoginIdentitySourceDto, 'type'> & { type: SocialSourceType }
export type UpdateSocialIdentitySourceDto = Omit<UpdateLoginIdentitySourceDto, 'type'> & { type?: SocialSourceType }
export type { LoginIdentitySourceStats as SocialIdentitySourceStats } from './loginIdentitySource'
