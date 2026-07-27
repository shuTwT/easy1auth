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
  list: ():Promise<CustomDomain[]> => request.get('/custom-domains'),
  
  create: (data: CreateDomainDto):Promise<CustomDomain> => 
    request.post('/custom-domains', data),
  
  verify: (id: string):Promise<CustomDomain>  => 
    request.post(`/custom-domains/${id}/verify`),
  
  updateSSL: (id: string, data: UpdateSSLDto):Promise<CustomDomain>  => 
    request.put(`/custom-domains/${id}/ssl`, data),
  
  delete: (id: string):Promise<CustomDomain>  => 
    request.delete(`/custom-domains/${id}`),
}
