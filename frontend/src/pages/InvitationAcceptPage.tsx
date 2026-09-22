import { useMutation } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { projectApi } from '../services/projectApi'
import { useAuthStore } from '../store/authStore'

export function InvitationAcceptPage() {
  const { token } = useParams<{ token: string }>()
  const navigate = useNavigate()
  const accessToken = useAuthStore((s) => s.accessToken)
  const attempted = useRef(false)

  const acceptMutation = useMutation({
    mutationFn: () => projectApi.acceptInvitation(token!),
    onSuccess: () => navigate('/projects'),
  })

  useEffect(() => {
    if (accessToken && token && !attempted.current) {
      attempted.current = true
      acceptMutation.mutate()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken, token])

  if (!accessToken) {
    return (
      <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-slate-50 px-4 text-center">
        <p className="text-slate-600">초대를 수락하려면 먼저 로그인해주세요.</p>
        <Link
          to="/login"
          state={{ returnTo: `/invitations/${token}` }}
          className="rounded-lg bg-primary-600 px-3.5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-primary-700"
        >
          로그인
        </Link>
      </main>
    )
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-3 bg-slate-50 px-4 text-center">
      {acceptMutation.isPending && <p className="text-sm text-slate-500">초대 수락 중...</p>}
      {acceptMutation.isError && <p className="text-sm text-red-500">초대가 만료되었거나 유효하지 않습니다.</p>}
      {acceptMutation.isSuccess && <p className="text-sm text-emerald-600">참가 완료! 이동 중...</p>}
    </main>
  )
}
