import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { authApi } from '../services/authApi'
import { projectApi, type Project, type ProjectRole, type ProjectStatus } from '../services/projectApi'

const PROJECT_STATUSES: ProjectStatus[] = ['PLANNING', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'ARCHIVED']
const inputClass = 'rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm outline-none transition focus:border-primary-500 focus:ring-2 focus:ring-primary-100'

function ProjectSettingsForm({ project, role }: { project: Project; role: ProjectRole }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [name, setName] = useState(project.name)
  const [description, setDescription] = useState(project.description ?? '')
  const [status, setStatus] = useState(project.status)
  const [startDate, setStartDate] = useState(project.startDate ?? '')
  const [endDate, setEndDate] = useState(project.endDate ?? '')
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  const updateMutation = useMutation({
    mutationFn: () => projectApi.update(project.id, {
      name: name.trim(),
      description,
      status,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
      clearStartDate: !startDate,
      clearEndDate: !endDate,
    }),
    onSuccess: () => {
      setError(null)
      setSaved(true)
      queryClient.invalidateQueries({ queryKey: ['project', project.id] })
      queryClient.invalidateQueries({ queryKey: ['projects'] })
    },
    onError: (err: any) => {
      setSaved(false)
      setError(err.response?.data?.message ?? '프로젝트 수정에 실패했습니다.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => projectApi.remove(project.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      navigate('/projects')
    },
    onError: (err: any) => setError(err.response?.data?.message ?? '프로젝트 삭제에 실패했습니다.'),
  })

  const canEdit = role === 'OWNER' || role === 'ADMIN'
  if (!canEdit) return null

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        setSaved(false)
        updateMutation.mutate()
      }}
      className="rounded-2xl border border-slate-200/90 bg-white p-5 shadow-[0_2px_8px_rgba(15,23,42,0.06)] sm:p-6"
    >
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="font-semibold text-slate-900">기본 정보</h2>
          <p className="mt-1 text-xs text-slate-500">프로젝트 목록과 대시보드에 표시되는 정보를 관리합니다.</p>
        </div>
        {saved && <span className="text-xs font-medium text-emerald-600">저장되었습니다.</span>}
      </div>

      <div className="mt-5 grid gap-3">
        <label className="grid gap-1.5 text-xs font-semibold text-slate-600">
          프로젝트 이름
          <input value={name} maxLength={200} onChange={(event) => setName(event.target.value)} aria-label="프로젝트 이름" className={inputClass} />
        </label>
        <label className="grid gap-1.5 text-xs font-semibold text-slate-600">
          설명
          <textarea value={description} onChange={(event) => setDescription(event.target.value)} rows={4} aria-label="프로젝트 설명" className={inputClass} />
        </label>
        <label className="grid gap-1.5 text-xs font-semibold text-slate-600">
          상태
          <select value={status} onChange={(event) => setStatus(event.target.value as ProjectStatus)} aria-label="프로젝트 상태" className={inputClass}>
            {PROJECT_STATUSES.map((value) => <option key={value}>{value}</option>)}
          </select>
        </label>
        <div className="grid gap-3 sm:grid-cols-2">
          <label className="grid gap-1.5 text-xs font-semibold text-slate-600">
            시작일
            <input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} aria-label="프로젝트 시작일" className={inputClass} />
          </label>
          <label className="grid gap-1.5 text-xs font-semibold text-slate-600">
            종료일
            <input type="date" value={endDate} min={startDate || undefined} onChange={(event) => setEndDate(event.target.value)} aria-label="프로젝트 종료일" className={inputClass} />
          </label>
        </div>
      </div>

      {error && <p className="mt-3 text-sm text-red-500" role="alert">{error}</p>}
      <div className="mt-5 flex flex-wrap items-center gap-3">
        <button type="submit" disabled={!name.trim() || updateMutation.isPending} className="rounded-xl bg-primary-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-50">
          {updateMutation.isPending ? '저장 중...' : '변경 저장'}
        </button>
        {role === 'OWNER' && (
          <button type="button" disabled={deleteMutation.isPending} onClick={() => window.confirm('프로젝트를 삭제할까요? 이 작업은 화면에서 복구할 수 없습니다.') && deleteMutation.mutate()} className="text-sm text-red-500 hover:underline disabled:opacity-50">
            프로젝트 삭제
          </button>
        )}
      </div>
    </form>
  )
}

export function ProjectSettingsPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const projectQuery = useQuery({ queryKey: ['project', id], queryFn: () => projectApi.get(id) })
  const membersQuery = useQuery({ queryKey: ['members', id], queryFn: () => projectApi.members(id) })
  const meQuery = useQuery({ queryKey: ['me'], queryFn: () => authApi.me().then((response) => response.data) })
  const currentRole = membersQuery.data?.find((member) => member.userId === meQuery.data?.id)?.role
  const isLoading = projectQuery.isLoading || membersQuery.isLoading || meQuery.isLoading
  const canEdit = currentRole === 'OWNER' || currentRole === 'ADMIN'

  return (
    <main className="mx-auto min-h-screen max-w-3xl bg-slate-50 px-4 py-5 sm:p-6">
      <Link to={`/projects/${id}`} className="inline-flex items-center text-sm font-medium text-slate-500 transition hover:text-slate-700">
        ← 프로젝트로
      </Link>
      <div className="mt-4">
        <h1 className="text-2xl font-bold text-slate-900">프로젝트 설정</h1>
        <p className="mt-1 text-sm text-slate-500">프로젝트 기본 정보와 운영 상태를 관리합니다.</p>
      </div>

      {isLoading && <p className="mt-6 text-sm text-slate-500">불러오는 중...</p>}
      {projectQuery.isError && <p className="mt-6 text-sm text-red-500">프로젝트 정보를 불러오지 못했습니다.</p>}
      {projectQuery.data && !isLoading && (
        canEdit && currentRole
          ? <div className="mt-6"><ProjectSettingsForm key={projectQuery.data.updatedAt} project={projectQuery.data} role={currentRole} /></div>
          : <section className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">프로젝트 설정은 관리자 또는 소유자만 변경할 수 있습니다.</section>
      )}
    </main>
  )
}
