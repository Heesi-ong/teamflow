import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../services/authApi'
import { useAuthStore } from '../store/authStore'

export function DashboardPage() {
  const navigate = useNavigate()
  const setAccessToken = useAuthStore((s) => s.setAccessToken)
  const { data, isLoading, isError } = useQuery({
    queryKey: ['me'],
    queryFn: () => authApi.me().then((res) => res.data),
    retry: false,
  })

  async function handleLogout() {
    await authApi.logout().catch(() => {})
    setAccessToken(null)
    navigate('/login')
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-3 bg-slate-50">
      <h1 className="text-2xl font-bold text-blue-600">TeamFlow</h1>
      {isLoading && <p className="text-slate-500">확인 중...</p>}
      {isError && <p className="text-red-500">인증 확인 실패. 다시 로그인해주세요.</p>}
      {data && (
        <p className="text-slate-600">
          {data.name}님 ({data.email}) 환영합니다.
        </p>
      )}
      <button onClick={handleLogout} className="rounded bg-slate-600 px-3 py-2 text-white">
        로그아웃
      </button>
    </main>
  )
}
