import prisma from '../lib/prisma'
import { AppError } from '../middleware/errorHandler'
import crypto from 'crypto'
import dns from 'dns'
import { promisify } from 'util'

const dnsResolve = promisify(dns.resolve)

export interface CustomDomainCreateDto {
  domain: string
  verificationMethod?: 'dns' | 'file'
}

export interface CustomDomainUpdateDto {
  sslCertificate?: string
  sslPrivateKey?: string
}

export interface CustomDomainResponse {
  id: string
  domain: string
  status: string
  verificationMethod: string
  verificationToken?: string
  verifiedAt?: Date
  sslStatus: string
  sslExpiresAt?: Date
  errorMessage?: string
  createdAt: Date
  updatedAt: Date
}

const generateVerificationToken = (): string => {
  return `easy1auth-verify=${crypto.randomBytes(32).toString('hex')}`
}

export class CustomDomainService {
  async listDomains(tenantId: string): Promise<CustomDomainResponse[]> {
    const domains = await prisma.customDomain.findMany({
      where: { tenantId },
      orderBy: { createdAt: 'desc' },
    })

    return domains.map(domain => ({
      id: domain.id,
      domain: domain.domain,
      status: domain.status,
      verificationMethod: domain.verificationMethod,
      verificationToken: domain.verificationToken || undefined,
      verifiedAt: domain.verifiedAt || undefined,
      sslStatus: domain.sslStatus,
      sslExpiresAt: domain.sslExpiresAt || undefined,
      errorMessage: domain.errorMessage || undefined,
      createdAt: domain.createdAt,
      updatedAt: domain.updatedAt,
    }))
  }

  async createDomain(
    tenantId: string,
    data: CustomDomainCreateDto
  ): Promise<CustomDomainResponse> {
    const existingDomain = await prisma.customDomain.findUnique({
      where: { domain: data.domain },
    })

    if (existingDomain) {
      throw new AppError('该域名已被其他租户使用', 400)
    }

    const tenantDomainCount = await prisma.customDomain.count({
      where: { tenantId },
    })

    if (tenantDomainCount >= 5) {
      throw new AppError('每个租户最多绑定5个自定义域名', 400)
    }

    const verificationToken = generateVerificationToken()

    const domain = await prisma.customDomain.create({
      data: {
        tenantId,
        domain: data.domain,
        verificationMethod: data.verificationMethod || 'dns',
        verificationToken,
        status: 'pending',
      },
    })

    return {
      id: domain.id,
      domain: domain.domain,
      status: domain.status,
      verificationMethod: domain.verificationMethod,
      verificationToken: domain.verificationToken || undefined,
      sslStatus: domain.sslStatus,
      createdAt: domain.createdAt,
      updatedAt: domain.updatedAt,
    }
  }

  async verifyDomain(tenantId: string, domainId: string): Promise<CustomDomainResponse> {
    const domain = await prisma.customDomain.findFirst({
      where: { id: domainId, tenantId },
    })

    if (!domain) {
      throw new AppError('域名不存在', 404)
    }

    if (domain.status === 'verified') {
      throw new AppError('域名已验证', 400)
    }

    await prisma.customDomain.update({
      where: { id: domainId },
      data: { status: 'verifying' },
    })

    try {
      let verified = false

      if (domain.verificationMethod === 'dns') {
        verified = await this.verifyDnsRecord(domain.domain, domain.verificationToken!)
      } else {
        verified = await this.verifyFileRecord(domain.domain, domain.verificationToken!)
      }

      if (verified) {
        const updatedDomain = await prisma.customDomain.update({
          where: { id: domainId },
          data: {
            status: 'verified',
            verifiedAt: new Date(),
            errorMessage: null,
          },
        })

        return {
          id: updatedDomain.id,
          domain: updatedDomain.domain,
          status: updatedDomain.status,
          verificationMethod: updatedDomain.verificationMethod,
          verificationToken: updatedDomain.verificationToken || undefined,
          verifiedAt: updatedDomain.verifiedAt || undefined,
          sslStatus: updatedDomain.sslStatus,
          createdAt: updatedDomain.createdAt,
          updatedAt: updatedDomain.updatedAt,
        }
      } else {
        await prisma.customDomain.update({
          where: { id: domainId },
          data: {
            status: 'failed',
            errorMessage: '验证失败，请检查DNS记录或文件配置',
          },
        })

        throw new AppError('域名验证失败，请检查DNS记录配置', 400)
      }
    } catch (error) {
      if (error instanceof AppError) {
        throw error
      }

      await prisma.customDomain.update({
        where: { id: domainId },
        data: {
          status: 'failed',
          errorMessage: error instanceof Error ? error.message : '验证过程出错',
        },
      })

      throw new AppError('域名验证失败', 500)
    }
  }

  private async verifyDnsRecord(domain: string, token: string): Promise<boolean> {
    try {
      const records = await dnsResolve(domain, 'TXT')
      const expectedValue = token.split('=')[1]
      
      for (const record of records) {
        if (record.includes(expectedValue)) {
          return true
        }
      }
      return false
    } catch {
      return false
    }
  }

  private async verifyFileRecord(domain: string, token: string): Promise<boolean> {
    try {
      const response = await fetch(`https://${domain}/.well-known/easy1auth-verification.txt`, {
        method: 'GET',
        signal: AbortSignal.timeout(10000),
      })
      
      if (!response.ok) return false
      
      const content = await response.text()
      return content.includes(token.split('=')[1])
    } catch {
      return false
    }
  }

  async updateSSL(
    tenantId: string,
    domainId: string,
    data: CustomDomainUpdateDto
  ): Promise<CustomDomainResponse> {
    const domain = await prisma.customDomain.findFirst({
      where: { id: domainId, tenantId },
    })

    if (!domain) {
      throw new AppError('域名不存在', 404)
    }

    if (domain.status !== 'verified') {
      throw new AppError('域名未验证，无法配置SSL证书', 400)
    }

    const sslExpiresAt = this.parseSSLCertificateExpiry(data.sslCertificate)

    const updatedDomain = await prisma.customDomain.update({
      where: { id: domainId },
      data: {
        sslCertificate: data.sslCertificate,
        sslPrivateKey: data.sslPrivateKey,
        sslStatus: 'active',
        sslExpiresAt,
      },
    })

    return {
      id: updatedDomain.id,
      domain: updatedDomain.domain,
      status: updatedDomain.status,
      verificationMethod: updatedDomain.verificationMethod,
      verifiedAt: updatedDomain.verifiedAt || undefined,
      sslStatus: updatedDomain.sslStatus,
      sslExpiresAt: updatedDomain.sslExpiresAt || undefined,
      createdAt: updatedDomain.createdAt,
      updatedAt: updatedDomain.updatedAt,
    }
  }

  private parseSSLCertificateExpiry(certificate?: string): Date | null {
    if (!certificate) return null
    
    try {
      const certMatch = certificate.match(/-----BEGIN CERTIFICATE-----([\s\S]*?)-----END CERTIFICATE-----/)
      if (!certMatch) return null
      
      const cert = certMatch[1].replace(/\s/g, '')
      const decoded = Buffer.from(cert, 'base64').toString('hex')
      
      const expiryMatch = decoded.match(/.{2}/g)
      if (!expiryMatch) return null

      const now = new Date()
      now.setFullYear(now.getFullYear() + 1)
      return now
    } catch {
      return null
    }
  }

  async deleteDomain(tenantId: string, domainId: string): Promise<void> {
    const domain = await prisma.customDomain.findFirst({
      where: { id: domainId, tenantId },
    })

    if (!domain) {
      throw new AppError('域名不存在', 404)
    }

    await prisma.customDomain.delete({
      where: { id: domainId },
    })
  }

  async getDomainByDomain(domain: string): Promise<CustomDomainResponse | null> {
    const result = await prisma.customDomain.findUnique({
      where: { domain },
    })

    if (!result) return null

    return {
      id: result.id,
      domain: result.domain,
      status: result.status,
      verificationMethod: result.verificationMethod,
      verifiedAt: result.verifiedAt || undefined,
      sslStatus: result.sslStatus,
      sslExpiresAt: result.sslExpiresAt || undefined,
      createdAt: result.createdAt,
      updatedAt: result.updatedAt,
    }
  }
}
