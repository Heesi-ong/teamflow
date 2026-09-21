import axios from 'axios'

// 09-authentication-authorization.md: Access Token은 메모리 보관, 인터셉터에서
// 401 시 /api/auth/refresh 재시도. 실제 토큰 주입/재발급 로직은 Phase 2에서 추가한다.
export const http = axios.create({
  baseURL: '/api',
})
