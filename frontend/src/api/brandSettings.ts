import request from '../utils/request'
import type {
  BrandSettings,
  UpdateBrandSettingsDto,
  BrandSettingsResponse,
} from '../types/brandSettings'

export const brandSettingsApi = {
  get(): Promise<BrandSettingsResponse> {
    return request.get('/brand-settings')
  },

  update(data: UpdateBrandSettingsDto): Promise<BrandSettingsResponse> {
    return request.put('/brand-settings', data)
  },

  getPublic(domain?: string): Promise<BrandSettings> {
    return request.get('/brand-settings/public', {
      params: { domain },
    })
  },
}
