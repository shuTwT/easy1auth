import request from '../utils/request'
import type {
  BrandSettings,
  UpdateBrandSettingsDto,
  BrandSettingsResponse,
} from '../types/brandSettings'

export const brandSettingsApi = {
  get() {
    return request.get<BrandSettingsResponse>('/brand-settings')
  },

  update(data: UpdateBrandSettingsDto) {
    return request.put<BrandSettingsResponse>('/brand-settings', data)
  },

  getPublic(domain?: string) {
    return request.get<BrandSettings>('/brand-settings/public', {
      params: { domain },
    })
  },
}
