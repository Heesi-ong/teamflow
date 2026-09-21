import { useQueryClient, useQuery, useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { projectApi } from '../services/projectApi'

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
      <Link to={`/projects/${id}`} className="text-sm text-blue-600 underline">
        ← 프로젝트로
      </Link>
      <h1 className="mt-4 text-2xl font-bold text-slate-800">팀원 관리</h1>

      <section className="mt-4 rounded border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="font-semibold text-slate-700">팀원 초대</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            inviteMutation.mutate()
          }}
          className="mt-2 flex gap-2"
        >
          <input
            type="email"
            placeholder="이메일 (비우면 초대 링크만 생성)"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="flex-1 rounded border border-slate-300 px-3 py-2"
          />
          <button type="submit" disabled={inviteMutation.isPending} className="rounded bg-blue-600 px-3 py-2 text-white">
            초대
          </button>
        </form>
        {error && <p className="mt-2 text-sm text-red-500">{error}</p>}
        {inviteLink && (
          <p className="mt-2 break-all text-sm text-slate-500">
            초대 링크: <span className="text-blue-600">{inviteLink}</span>
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

      <section className="mt-4 rounded border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="font-semibold text-slate-700">팀원 목록</h2>
        <ul className="mt-2 divide-y divide-slate-100">
          {membersQuery.data?.map((member) => (
            <li key={member.id} className="flex items-center justify-between py-2">
              <span className="text-slate-700">
                {member.userName} ({member.userEmail}) — {member.role}
              </span>
              {member.role !== 'OWNER' && (
                <button
                  onClick={() => removeMutation.mutate(member.id)}
                  className="text-sm text-red-500 hover:underline"
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
