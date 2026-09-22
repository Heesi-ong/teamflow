import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { projectApi, type Project, type ProjectRole, type ProjectStatus } from '../services/projectApi'
import { authApi } from '../services/authApi'

const NAV_LINKS = (id: number) => [
  { to: `/projects/${id}/dashboard`, label: 'Dashboard' },
  { to: `/projects/${id}/board`, label: 'Kanban Board' },
  { to: `/projects/${id}/calendar`, label: 'Calendar' },
  { to: `/projects/${id}/chat`, label: '채팅' },
  { to: `/projects/${id}/documents`, label: '문서' },
  { to: `/projects/${id}/files`, label: '파일' },
  { to: `/projects/${id}/members`, label: '팀원 관리' },
]

const PROJECT_STATUSES: ProjectStatus[] = ['PLANNING', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'ARCHIVED']

function ProjectSettings({ project, role }: { project: Project; role?: ProjectRole }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [name, setName] = useState(project.name)
  const [description, setDescription] = useState(project.description ?? '')
  const [status, setStatus] = useState(project.status)
  const [startDate, setStartDate] = useState(project.startDate ?? '')
  const [endDate, setEndDate] = useState(project.endDate ?? '')
  const [error, setError] = useState<string | null>(null)
  const canEdit = role === 'OWNER' || role === 'ADMIN'

  const updateMutation = useMutation({
    mutationFn: () => projectApi.update(project.id, {
      name,
      description,
      status,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    }),
    onSuccess: () => {
      setError(null)
      queryClient.invalidateQueries({ queryKey: ['project', project.id] })
      queryClient.invalidateQueries({ queryKey: ['projects'] })
    },
    onError: (err: any) => setError(err.response?.data?.message ?? '프로젝트 수정에 실패했습니다.'),
  })

  const deleteMutation = useMutation({
    mutationFn: () => projectApi.remove(project.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      navigate('/projects')
    },
  })

  if (!canEdit) return null

  return (
    <section className="mt-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="font-semibold text-slate-900">프로젝트 설정</h2>
      <div className="mt-3 grid gap-3">
        <input value={name} maxLength={200} onChange={(e) => setName(e.target.value)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm" />
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} className="rounded-lg border border-slate-300 px-3 py-2 text-sm" />
        <select value={status} onChange={(e) => setStatus(e.target.value as ProjectStatus)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm">
          {PROJECT_STATUSES.map((value) => <option key={value}>{value}</option>)}
        </select>
        <div className="grid grid-cols-2 gap-3">
          <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <input type="date" value={endDate} min={startDate || undefined} onChange={(e) => setEndDate(e.target.value)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm" />
        </div>
      </div>
      {error && <p className="mt-2 text-sm text-red-500">{error}</p>}
      <div className="mt-4 flex items-center gap-3">
        <button disabled={!name.trim() || updateMutation.isPending} onClick={() => updateMutation.mutate()} className="rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-semibold text-white disabled:opacity-50">
          변경 저장
        </button>
        {role === 'OWNER' && (
          <button onClick={() => window.confirm('프로젝트를 삭제할까요? 이 작업은 화면에서 복구할 수 없습니다.') && deleteMutation.mutate()} className="text-sm text-red-500 hover:underline">
            프로젝트 삭제
          </button>
        )}
      </div>
    </section>
  )
}

export function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const membersQuery = useQuery({ queryKey: ['members', id], queryFn: () => projectApi.members(id) })
  const meQuery = useQuery({ queryKey: ['me'], queryFn: () => authApi.me().then((res) => res.data) })
  const { data, isLoading, isError } = useQuery({
    queryKey: ['project', id],
    queryFn: () => projectApi.get(id),
  })
  const currentRole = membersQuery.data?.find((member) => member.userId === meQuery.data?.id)?.role

  return (
    <main className="mx-auto min-h-screen max-w-2xl bg-slate-50 p-6">
      <Link to="/projects" className="text-sm font-medium text-slate-500 hover:text-slate-700">
        ← 프로젝트 목록
      </Link>
      {isLoading && <p className="mt-4 text-sm text-slate-500">불러오는 중...</p>}
      {isError && <p className="mt-4 text-sm text-red-500">프로젝트를 찾을 수 없거나 접근 권한이 없습니다.</p>}
      {data && (
        <>
        <div className="mt-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-bold text-slate-900">{data.name}</h1>
            <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">{data.status}</span>
          </div>
          <p className="mt-2 text-slate-600">{data.description || '설명 없음'}</p>
          <p className="mt-2 text-sm text-slate-400">
            {data.startDate ?? '?'} ~ {data.endDate ?? '?'}
          </p>
          <div className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-3">
            {NAV_LINKS(data.id).map((link) => (
              <Link
                key={link.to}
                to={link.to}
                className="rounded-lg border border-slate-200 px-3 py-2.5 text-center text-sm font-medium text-slate-700 transition hover:border-primary-300 hover:bg-primary-50 hover:text-primary-700"
              >
                {link.label}
              </Link>
            ))}
          </div>
        </div>
        <ProjectSettings key={data.updatedAt} project={data} role={currentRole} />
        </>
      )}
    </main>
  )
}
