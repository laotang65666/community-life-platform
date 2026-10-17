import { request } from './http'
import type { LoginData } from './types'

export function sendSmsCode(phone: string): Promise<null> {
  return request<null>({
    method: 'POST',
    url: '/auth/sms-code',
    data: { phone }
  })
}

export function login(phone: string, code: string): Promise<LoginData> {
  return request<LoginData>({
    method: 'POST',
    url: '/auth/login',
    data: { phone, code }
  })
}
