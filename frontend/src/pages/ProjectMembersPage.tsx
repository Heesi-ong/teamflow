import { useQueryClient, useQuery, useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { projectApi, type ProjectRole } from '../services/projectApi'
import { authApi } from '../services/authApi'

const inputClass =
  'flex-1 rounded-lg border border-slate-300 px-3.5 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100'

export function ProjectMembersPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [inviteLink, setInviteLink] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const membersQuery = useQuery({ queryKey: ['members', id], queryFn: () => projectApi.members(id) })
  const meQuery = useQuery({ queryKey: ['me'], queryFn: () => authApi.me().then((res) => res.data) })
  const currentMember = membersQuery.data?.find((member) => member.userId === meQuery.data?.id)
  const canManageMembers = currentMember?.role === 'OWNER' || currentMember?.role === 'ADMIN'
  const isOwner = currentMember?.role === 'OWNER'
  const invitationsQuery = useQuery({
    queryKey: ['invitations', id],
    queryFn: () => projectApi.invitations(id),
    enabled: canManageMembers,
  })

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

  const revokeMutation = useMutation({
    mutationFn: (invitationId: number) => projectApi.revokeInvitation(id, invitationId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['invitations', id] }),
  })

  const roleMutation = useMutation({
    mutationFn: ({ memberId, role }: { memberId: number; role: Exclude<ProjectRole, 'OWNER'> }) =>
      projectApi.changeRole(id, memberId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['members', id] })
      queryClient.invalidateQueries({ queryKey: ['dashboard', id] })
    },
  })

  const transferMutation = useMutation({
    mutationFn: (memberId: number) => projectApi.transferOwnership(id, memberId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['members', id] })
      queryClient.invalidateQueries({ queryKey: ['project', id] })
      queryClient.invalidateQueries({ queryKey: ['dashboard', id] })
    },
  })

  const leaveMutation = useMutation({
    mutationFn: () => projectApi.leave(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      navigate('/projects')
    },
  })

  return (
    <main className="mx-auto min-h-screen max-w-2xl bg-slate-50 p-6">
      <Link to={`/projects/${id}`} className="text-sm font-medium text-slate-500 hover:text-slate-700">
        ← 프로젝트로
      </Link>
      <h1 className="mt-2 text-xl font-bold text-slate-900">팀원 관리</h1>

      {canManageMembers && <section className="mt-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
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
            maxLength={255}
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
              <li key={inv.id} className="flex items-center justify-between gap-2">
                <span>{inv.email ?? '(링크 초대)'} — {inv.role} — {inv.status}</span>
                {inv.status === 'PENDING' && canManageMembers && (
                  <button onClick={() => revokeMutation.mutate(inv.id)} className="text-xs text-red-500 hover:underline">
                    취소
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>}

      <section className="mt-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-slate-700">팀원 목록</h2>
        <ul className="mt-2 divide-y divide-slate-100">
          {membersQuery.data?.map((member) => (
            <li key={member.id} className="flex items-center justify-between py-2.5">
              <span className="min-w-0 flex-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">{member.userName}</span>{' '}
                <span className="text-slate-400">({member.userEmail})</span> —{' '}
                <span className="font-medium text-slate-500">{member.role}</span>
              </span>
              <div className="flex items-center gap-2">
                {isOwner && member.role !== 'OWNER' && (
                  <select
                    aria-label={`${member.userName} 역할`}
                    value={member.role}
                    onChange={(e) => roleMutation.mutate({
                      memberId: member.id,
                      role: e.target.value as Exclude<ProjectRole, 'OWNER'>,
                    })}
                    className="rounded border border-slate-200 px-2 py-1 text-xs"
                  >
                    <option value="ADMIN">ADMIN</option>
                    <option value="MEMBER">MEMBER</option>
                    <option value="GUEST">GUEST</option>
                  </select>
                )}
                {isOwner && member.role !== 'OWNER' && (
                  <button
                    onClick={() => window.confirm(`${member.userName}님에게 소유권을 위임할까요?`) && transferMutation.mutate(member.id)}
                    className="text-xs text-primary-600 hover:underline"
                  >
                    소유권 위임
                  </button>
                )}
                {canManageMembers && member.role !== 'OWNER' && member.userId !== meQuery.data?.id && (
                  <button
                    onClick={() => window.confirm(`${member.userName}님을 제거할까요?`) && removeMutation.mutate(member.id)}
                    className="text-sm text-slate-400 hover:text-red-500"
                  >
                    제거
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      </section>

      {currentMember && currentMember.role !== 'OWNER' && (
        <button
          onClick={() => window.confirm('프로젝트에서 탈퇴할까요?') && leaveMutation.mutate()}
          className="mt-4 text-sm text-red-500 hover:underline"
        >
          프로젝트 탈퇴
        </button>
      )}
    </main>
  )
}
