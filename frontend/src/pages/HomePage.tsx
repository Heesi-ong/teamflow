import { useQuery } from '@tanstack/react-query'
import axios from 'axios'

type HealthResponse = {
  status: string
}

function useBackendHealth() {
  return useQuery({
    queryKey: ['backend-health'],
    queryFn: async () => {
      const { data } = await axios.get<HealthResponse>('/actuator/health')
      return data
    },
    retry: false,
  })
}

export function HomePage() {
  const { data, isLoading, isError } = useBackendHealth()

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-3 bg-slate-50">
      <h1 className="text-2xl font-bold text-blue-600">TeamFlow</h1>
      <p className="text-slate-500">웹 기반 팀 프로젝트 협업 플랫폼 — Phase 1 스캐폴딩</p>
      <p className="text-sm text-slate-400">
        Backend health:{' '}
        {isLoading && '확인 중...'}
        {isError && <span className="text-red-500">연결 실패 (backend 미기동 또는 프록시 확인 필요)</span>}
        {data && <span className="text-green-600">{data.status}</span>}
      </p>
    </main>
  )
}
