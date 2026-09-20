import request from '@/utils/request'

export interface SystemInitializationInput {
  account: string
  password: string
}

export interface SystemInitializationStatus {
  initialized: boolean
}

let cachedStatus: boolean | undefined
let statusRequest: Promise<boolean> | undefined

export const systemInitializationApi = {
  async getStatus(force = false): Promise<boolean> {
    if (!force && cachedStatus !== undefined) {
      return cachedStatus
    }
    if (!force && statusRequest) {
      return statusRequest
    }
    statusRequest = request
      .get<never, SystemInitializationStatus>('/system/initialization/status')
      .then((status) => {
        cachedStatus = status.initialized
        return status.initialized
      })
      .finally(() => {
        statusRequest = undefined
      })
    return statusRequest
  },

  async initialize(input: SystemInitializationInput): Promise<void> {
    await request.post('/system/initialization', input)
    cachedStatus = true
  },
}
