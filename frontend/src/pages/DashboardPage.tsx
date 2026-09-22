import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
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
    <main className="flex min-h-screen flex-col items-center justify-center bg-slate-50 px-4">
      <Link to="/" className="mb-8 text-xl font-bold tracking-tight text-primary-600">
        TeamFlow
      </Link>
      <div className="w-full max-w-sm rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        {isLoading && <p className="text-sm text-slate-500">확인 중...</p>}
        {isError && <p className="text-sm text-red-500">인증 확인 실패. 다시 로그인해주세요.</p>}
        {data && (
          <p className="text-slate-600">
            <span className="font-semibold text-slate-900">{data.name}</span>님 환영합니다.
            <br />
            <span className="text-sm text-slate-400">{data.email}</span>
          </p>
        )}
        <div className="mt-6 flex flex-col gap-2.5">
          <Link
            to="/projects"
            className="rounded-lg bg-primary-600 px-3.5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700"
          >
            내 프로젝트
          </Link>
          <button
            onClick={handleLogout}
            className="rounded-lg border border-slate-200 px-3.5 py-2.5 text-sm font-semibold text-slate-600 transition hover:bg-slate-50"
          >
            로그아웃
          </button>
        </div>
      </div>
    </main>
  )
}
