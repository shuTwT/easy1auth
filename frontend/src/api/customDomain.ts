import request from '@/utils/request'

export interface CustomDomain {
  id: string
  domain: string
  status: string
  verificationMethod: string
  verificationToken?: string
  verifiedAt?: string
  sslStatus: string
  sslExpiresAt?: string
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export interface CreateDomainDto {
  domain: string
  verificationMethod?: 'dns' | 'file'
}

export interface UpdateSSLDto {
  sslCertificate: string
  sslPrivateKey: string
}

export const customDomainApi = {
  list: () => request.get<{ data: CustomDomain[] }>('/custom-domains'),
  
  create: (data: CreateDomainDto) => 
    request.post<{ data: CustomDomain }>('/custom-domains', data),
  
  verify: (id: string) => 
    request.post<{ data: CustomDomain }>(`/custom-domains/${id}/verify`),
  
  updateSSL: (id: string, data: UpdateSSLDto) => 
    request.put<{ data: CustomDomain }>(`/custom-domains/${id}/ssl`, data),
  
  delete: (id: string) => 
    request.delete(`/custom-domains/${id}`),
}
