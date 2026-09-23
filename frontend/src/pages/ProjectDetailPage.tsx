import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { authApi } from '../services/authApi'
import { chatApi } from '../services/chatApi'
import { dashboardApi } from '../services/dashboardApi'
import { projectApi, type Project, type ProjectRole, type ProjectStatus } from '../services/projectApi'

const NAV_LINKS = (id: number) => [
  { to: `/projects/${id}/board`, label: 'Kanban Board' },
  { to: `/projects/${id}/calendar`, label: 'Calendar' },
  { to: `/projects/${id}/chat`, label: '채팅' },
  { to: `/projects/${id}/documents`, label: '문서' },
  { to: `/projects/${id}/files`, label: '파일' },
  { to: `/projects/${id}/members`, label: '팀원 관리' },
]

const PROJECT_STATUSES: ProjectStatus[] = ['PLANNING', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'ARCHIVED']

function StatCard({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 text-center shadow-sm">
      <p className="text-2xl font-bold text-slate-900">{value}</p>
      <p className="mt-1 text-xs font-medium text-slate-500">{label}</p>
    </div>
  )
}

function WidgetCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <h2 className="text-sm font-semibold text-slate-700">{title}</h2>
      {children}
    </div>
  )
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function ProjectDashboard({ id }: { id: number }) {
  const [keyword, setKeyword] = useState('')
  const [searchTerm, setSearchTerm] = useState('')

  const dashboardQuery = useQuery({ queryKey: ['dashboard', id], queryFn: () => dashboardApi.get(id) })
  const chatQuery = useQuery({ queryKey: ['chat-preview', id], queryFn: () => chatApi.history(id, undefined, 5) })
  const searchQuery = useQuery({
    queryKey: ['search', id, searchTerm],
    queryFn: () => dashboardApi.search(id, searchTerm),
    enabled: searchTerm.length > 0,
  })

  const stats = dashboardQuery.data

  return (
    <>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          setSearchTerm(keyword.trim())
        }}
        className="mt-4 flex gap-2"
      >
        <input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Task / 문서 / 댓글 검색"
          className="flex-1 rounded-lg border border-slate-300 px-3.5 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100"
        />
        <button type="submit" className="rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-semibold text-white hover:bg-primary-700">
          검색
        </button>
      </form>

      {searchTerm && searchQuery.data && (
        <div className="mt-3 rounded-xl border border-slate-200 bg-white p-4 text-sm shadow-sm">
          <p className="font-semibold text-slate-900">"{searchTerm}" 검색 결과</p>
          <div className="mt-3 grid grid-cols-1 gap-4 md:grid-cols-3">
            <div>
              <p className="text-xs font-semibold text-slate-500">Task ({searchQuery.data.tasks.length})</p>
              <ul className="mt-1.5 space-y-1">
                {searchQuery.data.tasks.map((t) => (
                  <li key={t.id}>
                    <Link to={`/projects/${id}/board?taskId=${t.id}`} className="text-primary-600 hover:underline">
                      {t.title}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500">문서 ({searchQuery.data.documents.length})</p>
              <ul className="mt-1.5 space-y-1">
                {searchQuery.data.documents.map((d) => (
                  <li key={d.id} className="text-slate-700">
                    {d.title}
                  </li>
                ))}
              </ul>
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500">댓글 ({searchQuery.data.comments.length})</p>
              <ul className="mt-1.5 space-y-1">
                {searchQuery.data.comments.map((c) => (
                  <li key={c.id} className="truncate text-slate-700">
                    {c.content}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}

      {stats && (
        <>
          <div className="mt-4 grid grid-cols-2 gap-3 md:grid-cols-5">
            <StatCard label="전체" value={stats.totalTasks} />
            <StatCard label="TODO" value={stats.todoTasks} />
            <StatCard label="진행중" value={stats.inProgressTasks} />
            <StatCard label="완료" value={stats.doneTasks} />
            <StatCard label="팀원" value={stats.memberCount} />
          </div>

          <div className="mt-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between text-sm">
              <span className="font-semibold text-slate-700">진행률</span>
              <span className="text-slate-500">{stats.progressRate}%</span>
            </div>
            <div className="mt-2 h-2 w-full rounded-full bg-slate-100">
              <div className="h-2 rounded-full bg-primary-500" style={{ width: `${stats.progressRate}%` }} />
            </div>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
            <WidgetCard title="마감 임박 Task">
              <ul className="mt-2 space-y-1 text-sm">
                {stats.dueSoonTasks.map((t) => (
                  <li key={t.id} className="flex justify-between">
                    <Link to={`/projects/${id}/board?taskId=${t.id}`} className="truncate text-primary-600 hover:underline">
                      {t.title}
                    </Link>
                    <span className="shrink-0 text-slate-400">{t.dueDate}</span>
                  </li>
                ))}
                {stats.dueSoonTasks.length === 0 && <li className="text-slate-400">마감 임박 Task가 없습니다.</li>}
              </ul>
            </WidgetCard>

            <WidgetCard title="최근 채팅">
              <ul className="mt-2 space-y-1.5 text-sm">
                {chatQuery.data?.map((m) => (
                  <li key={m.id} className="truncate">
                    <span className="font-medium text-slate-700">{m.authorName}</span>{' '}
                    <span className="text-slate-500">{m.content}</span>
                  </li>
                ))}
                {chatQuery.data?.length === 0 && <li className="text-slate-400">채팅 내역이 없습니다.</li>}
              </ul>
              <Link to={`/projects/${id}/chat`} className="mt-2 inline-block text-xs font-medium text-primary-600 hover:underline">
                채팅으로 이동 →
              </Link>
            </WidgetCard>

            <WidgetCard title="최근 문서">
              <ul className="mt-2 space-y-1.5 text-sm">
                {stats.recentDocuments.map((d) => (
                  <li key={d.id} className="truncate">
                    <span className="text-slate-700">{d.title}</span>{' '}
                    <span className="text-xs text-slate-400">— {d.authorName}</span>
                  </li>
                ))}
                {stats.recentDocuments.length === 0 && <li className="text-slate-400">문서가 없습니다.</li>}
              </ul>
              <Link to={`/projects/${id}/documents`} className="mt-2 inline-block text-xs font-medium text-primary-600 hover:underline">
                문서로 이동 →
              </Link>
            </WidgetCard>

            <WidgetCard title="최근 파일">
              <ul className="mt-2 space-y-1.5 text-sm">
                {stats.recentFiles.map((f) => (
                  <li key={f.id} className="truncate">
                    <span className="text-slate-700">{f.fileName}</span>{' '}
                    <span className="text-xs text-slate-400">
                      — {f.uploaderName} · {formatSize(f.fileSize)}
                    </span>
                  </li>
                ))}
                {stats.recentFiles.length === 0 && <li className="text-slate-400">파일이 없습니다.</li>}
              </ul>
              <Link to={`/projects/${id}/files`} className="mt-2 inline-block text-xs font-medium text-primary-600 hover:underline">
                파일로 이동 →
              </Link>
            </WidgetCard>

            <WidgetCard title="최근 활동">
              <ul className="mt-2 space-y-1 text-sm">
                {stats.recentActivities.map((a) => (
                  <li key={a.id} className="text-slate-600">
                    {a.description}
                  </li>
                ))}
                {stats.recentActivities.length === 0 && <li className="text-slate-400">최근 활동이 없습니다.</li>}
              </ul>
            </WidgetCard>
          </div>
        </>
      )}
    </>
  )
}

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
      // 날짜 삭제는 명시적인 플래그로 전달한다. 필드 생략은 기존 날짜 유지다.
      startDate: startDate || undefined,
      endDate: endDate || undefined,
      clearStartDate: !startDate,
      clearEndDate: !endDate,
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
    <main className="mx-auto min-h-screen max-w-6xl bg-slate-50 p-6">
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
            <div className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-6">
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

          <ProjectDashboard id={id} />
          <ProjectSettings key={data.updatedAt} project={data} role={currentRole} />
        </>
      )}
    </main>
  )
}
