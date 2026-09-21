import { http } from './http'

export interface SignupRequest {
  email: string
  password: string
  name: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface TokenResponse {
  accessToken: string
  accessTokenExpiresIn: number
}

export interface UserResponse {
  id: number
  email: string
  name: string
  profileImageUrl: string | null
}

export const authApi = {
  signup: (body: SignupRequest) => http.post<{ id: number; email: string; name: string }>('/auth/signup', body),
  login: (body: LoginRequest) => http.post<TokenResponse>('/auth/login', body),
  logout: () => http.post<void>('/auth/logout'),
  me: () => http.get<UserResponse>('/users/me'),
}
