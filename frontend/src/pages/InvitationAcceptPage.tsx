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
      <main className="flex min-h-screen flex-col items-center justify-center gap-3 bg-slate-50">
        <p className="text-slate-600">초대를 수락하려면 먼저 로그인해주세요.</p>
        <Link to="/login" className="rounded bg-blue-600 px-3 py-2 text-white">
          로그인
        </Link>
      </main>
    )
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-3 bg-slate-50">
      {acceptMutation.isPending && <p className="text-slate-500">초대 수락 중...</p>}
      {acceptMutation.isError && <p className="text-red-500">초대가 만료되었거나 유효하지 않습니다.</p>}
      {acceptMutation.isSuccess && <p className="text-green-600">참가 완료! 이동 중...</p>}
    </main>
  )
}
