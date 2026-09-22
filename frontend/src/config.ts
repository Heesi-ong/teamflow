// Render처럼 프론트/백엔드가 서로 다른 origin에 배포될 때만 VITE_API_BASE_URL을 채운다.
// EC2(nginx 뒤 same-origin)나 로컬(Vite dev proxy)에서는 비워두면 기존처럼 상대 경로로 동작한다.
const rawApiBase = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') ?? ''

export const API_BASE_URL = rawApiBase

export const FILE_STORAGE_ENABLED = (import.meta.env.VITE_FILE_STORAGE_ENABLED as string | undefined) !== 'false'

export const WS_BASE_URL = rawApiBase
  ? rawApiBase.replace(/^http/, 'ws')
  : `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}`
