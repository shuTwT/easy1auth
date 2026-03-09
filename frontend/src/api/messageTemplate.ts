import request from '@/utils/request'

export interface MessageTemplate {
  id: string
  type: 'email' | 'sms'
  code: string
  name: string
  subject?: string
  content: string
  variables?: Record<string, string>
  isDefault: boolean
  status: string
  createdAt: string
  updatedAt: string
}

export interface CreateTemplateDto {
  type: 'email' | 'sms'
  code: string
  name: string
  subject?: string
  content: string
  variables?: Record<string, string>
  isDefault?: boolean
}

export interface UpdateTemplateDto {
  name?: string
  subject?: string
  content?: string
  variables?: Record<string, string>
  isDefault?: boolean
  status?: 'active' | 'inactive'
}

export const messageTemplateApi = {
  list: (type?: 'email' | 'sms') => 
    request.get<{ data: MessageTemplate[] }>('/message-templates', { params: { type } }),
  
  get: (id: string) => 
    request.get<{ data: MessageTemplate }>(`/message-templates/${id}`),
  
  create: (data: CreateTemplateDto) => 
    request.post<{ data: MessageTemplate }>('/message-templates', data),
  
  update: (id: string, data: UpdateTemplateDto) => 
    request.put<{ data: MessageTemplate }>(`/message-templates/${id}`, data),
  
  delete: (id: string) => 
    request.delete(`/message-templates/${id}`),
  
  initDefaults: () => 
    request.post('/message-templates/init'),
}
