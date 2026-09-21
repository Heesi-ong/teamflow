import { create } from 'zustand'

// 09-authentication-authorization.md: Access Token은 메모리에만 보관한다 (localStorage 금지).
interface AuthState {
  accessToken: string | null
  setAccessToken: (token: string | null) => void
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  setAccessToken: (token) => set({ accessToken: token }),
}))
