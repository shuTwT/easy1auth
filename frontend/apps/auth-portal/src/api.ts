import type { AuthInteractionContext } from '@easy1auth/types'

type ApiError = { code?: number; message?: string }

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    credentials: 'same-origin',
    ...init,
    headers: {
      Accept: 'application/json',
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...(init?.headers ?? {})
    }
  })
  const data = await response.json().catch(() => ({})) as T & ApiError
  if (!response.ok) throw new Error(data.message ?? '请求失败')
  if (typeof data.code === 'number' && data.code !== 0) throw new Error(data.message ?? '请求失败')
  return data
}

export const authPortalApi = {
  getInteraction(): Promise<AuthInteractionContext> {
    return request('/auth-portal-api/interaction')
  },
  getConsent(): Promise<AuthInteractionContext> {
    return request('/auth-portal-api/consent')
  },
  login(username: string, password: string, csrfToken?: string) {
    return request<{ status: 'success' | 'mfa_required'; redirectUrl?: string; expiresIn?: number }>('/auth-portal-api/login', {
      method: 'POST', body: JSON.stringify({ username, password }), headers: csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : undefined
    })
  },
  verifyMfa(code: string, csrfToken?: string) {
    return request<{ status: 'success'; redirectUrl?: string }>('/auth-portal-api/mfa', {
      method: 'POST', body: JSON.stringify({ code }), headers: csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : undefined
    })
  },
  consent(action: 'approve' | 'deny', csrfToken?: string) {
    return request<{ location: string }>('/auth-portal-api/consent', {
      method: 'POST', body: JSON.stringify({ action }), headers: csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : undefined
    })
  }
}
