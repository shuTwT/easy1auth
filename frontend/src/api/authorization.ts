import request from '@/utils/request'
import type { AuthorizationContext, MenuCatalogResponse } from '@/types/authorization'

export const authorizationApi = {
  getContext(): Promise<AuthorizationContext> {
    return request.get('/authorization/context')
  },

  getMenuCatalog(): Promise<MenuCatalogResponse> {
    return request.get('/platform/menus')
  },
}
