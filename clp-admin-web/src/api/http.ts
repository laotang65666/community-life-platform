import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

interface ApiResult<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('adminToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function clearAuthAndRedirect(): void {
  sessionStorage.removeItem('adminToken')
  sessionStorage.removeItem('adminPhone')
  if (window.location.hash !== '#/login') {
    window.location.hash = '#/login'
  }
}

function isAuthError(code?: number): boolean {
  return code === 2001 || code === 2003
}

/**
 * 统一请求入口:
 * 1. 请求拦截器自动带 token
 * 2. 这里解包 Result,code=0 返回 data
 * 3. 2001/2003 清理登录态并回登录页
 * 4. 其他错误统一提示
 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  let response
  try {
    response = await http.request<ApiResult<T>>(config)
  } catch (error) {
    const axiosError = error as AxiosError<ApiResult<unknown>>
    const body = axiosError.response?.data
    if (body && isAuthError(body.code)) {
      clearAuthAndRedirect()
    }
    const message = body?.message || axiosError.message || '网络异常'
    ElMessage.error(message)
    throw error
  }

  const body = response.data
  if (body && body.code === 0) {
    return body.data
  }

  if (body && isAuthError(body.code)) {
    clearAuthAndRedirect()
  }
  const message = body?.message || '请求失败'
  ElMessage.error(message)
  throw new Error(message)
}

export default http
