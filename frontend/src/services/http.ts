import axios from 'axios'
import { useAuthStore } from '../store/authStore'

// 09-authentication-authorization.md: Access Token은 메모리 보관, 인터셉터에서
// 401 시 /api/auth/refresh 재시도(Refresh Token은 HttpOnly Cookie로 자동 전송).
export const http = axios.create({
  baseURL: '/api',
  withCredentials: true,
})

http.interceptors.request.use((config) => {
  const accessToken = useAuthStore.getState().accessToken
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

let refreshPromise: Promise<string | null> | null = null

export async function refreshAccessToken(): Promise<string | null> {
  refreshPromise ??= axios
    .post<{ accessToken: string }>('/api/auth/refresh', null, { withCredentials: true })
    .then((res) => res.data.accessToken)
    .catch(() => null)
    .finally(() => {
      refreshPromise = null
    })
  return refreshPromise
}

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const newAccessToken = await refreshAccessToken()
      if (newAccessToken) {
        useAuthStore.getState().setAccessToken(newAccessToken)
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return http(originalRequest)
      }
      useAuthStore.getState().setAccessToken(null)
    }
    return Promise.reject(error)
  },
)
