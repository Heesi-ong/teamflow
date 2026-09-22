import { useQueryClient, useQuery, useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { projectApi } from '../services/projectApi'

const inputClass =
  'flex-1 rounded-lg border border-slate-300 px-3.5 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100'

export function ProjectMembersPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const queryClient = useQueryClient()
  const [email, setEmail] = useState('')
  const [inviteLink, setInviteLink] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const membersQuery = useQuery({ queryKey: ['members', id], queryFn: () => projectApi.members(id) })
  const invitationsQuery = useQuery({ queryKey: ['invitations', id], queryFn: () => projectApi.invitations(id) })

  const inviteMutation = useMutation({
    mutationFn: () => projectApi.invite(id, { email: email || undefined }),
    onSuccess: (invitation) => {
      setInviteLink(`${window.location.origin}/invitations/${invitation.token}`)
      setEmail('')
      setError(null)
      queryClient.invalidateQueries({ queryKey: ['invitations', id] })
    },
    onError: (err: any) => setError(err.response?.data?.message ?? '초대에 실패했습니다.'),
  })

  const removeMutation = useMutation({
    mutationFn: (memberId: number) => projectApi.removeMember(id, memberId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['members', id] }),
  })

  return (
    <main className="mx-auto min-h-screen max-w-2xl bg-slate-50 p-6">
      <Link to={`/projects/${id}`} className="text-sm font-medium text-slate-500 hover:text-slate-700">
        ← 프로젝트로
      </Link>
      <h1 className="mt-2 text-xl font-bold text-slate-900">팀원 관리</h1>

      <section className="mt-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-slate-700">팀원 초대</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            inviteMutation.mutate()
          }}
          className="mt-2.5 flex gap-2"
        >
          <input
            type="email"
            placeholder="이메일 (비우면 초대 링크만 생성)"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputClass}
          />
          <button
            type="submit"
            disabled={inviteMutation.isPending}
            className="rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-50"
          >
            초대
          </button>
        </form>
        {error && <p className="mt-2 text-sm text-red-500">{error}</p>}
        {inviteLink && (
          <p className="mt-2 break-all rounded-lg bg-primary-50 px-3 py-2 text-sm text-slate-600">
            초대 링크: <span className="text-primary-700">{inviteLink}</span>
          </p>
        )}
        {invitationsQuery.data && invitationsQuery.data.length > 0 && (
          <ul className="mt-3 space-y-1 text-sm text-slate-500">
            {invitationsQuery.data.map((inv) => (
              <li key={inv.id}>
                {inv.email ?? '(링크 초대)'} — {inv.role} — {inv.status}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="mt-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-slate-700">팀원 목록</h2>
        <ul className="mt-2 divide-y divide-slate-100">
          {membersQuery.data?.map((member) => (
            <li key={member.id} className="flex items-center justify-between py-2.5">
              <span className="text-sm text-slate-700">
                <span className="font-medium text-slate-900">{member.userName}</span>{' '}
                <span className="text-slate-400">({member.userEmail})</span> —{' '}
                <span className="font-medium text-slate-500">{member.role}</span>
              </span>
              {member.role !== 'OWNER' && (
                <button
                  onClick={() => removeMutation.mutate(member.id)}
                  className="text-sm text-slate-400 hover:text-red-500"
                >
                  제거
                </button>
              )}
            </li>
          ))}
        </ul>
      </section>
    </main>
  )
}
