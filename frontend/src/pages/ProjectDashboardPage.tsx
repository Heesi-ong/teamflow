import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { dashboardApi } from '../services/dashboardApi'

function StatCard({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded border border-slate-200 bg-white p-4 text-center shadow-sm">
      <p className="text-2xl font-bold text-slate-800">{value}</p>
      <p className="mt-1 text-xs text-slate-500">{label}</p>
    </div>
  )
}

export function ProjectDashboardPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const [keyword, setKeyword] = useState('')
  const [searchTerm, setSearchTerm] = useState('')

  const dashboardQuery = useQuery({ queryKey: ['dashboard', id], queryFn: () => dashboardApi.get(id) })
  const searchQuery = useQuery({
    queryKey: ['search', id, searchTerm],
    queryFn: () => dashboardApi.search(id, searchTerm),
    enabled: searchTerm.length > 0,
  })

  const stats = dashboardQuery.data

  return (
    <main className="min-h-screen bg-slate-50 p-6">
      <Link to={`/projects/${id}`} className="text-sm text-blue-600 underline">
        ← 프로젝트로
      </Link>
      <h1 className="mt-2 text-xl font-bold text-slate-800">Dashboard</h1>

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
          className="flex-1 rounded border border-slate-300 px-3 py-2 text-sm"
        />
        <button type="submit" className="rounded bg-blue-600 px-3 py-2 text-sm text-white">
          검색
        </button>
      </form>

      {searchTerm && searchQuery.data && (
        <div className="mt-3 rounded border border-slate-200 bg-white p-4 text-sm">
          <p className="font-semibold text-slate-700">"{searchTerm}" 검색 결과</p>
          <div className="mt-2 grid grid-cols-1 gap-3 md:grid-cols-3">
            <div>
              <p className="text-xs font-semibold text-slate-500">Task ({searchQuery.data.tasks.length})</p>
              <ul className="mt-1 space-y-1">
                {searchQuery.data.tasks.map((t) => (
                  <li key={t.id}>
                    <Link to={`/projects/${id}/board?taskId=${t.id}`} className="text-blue-600 hover:underline">
                      {t.title}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500">문서 ({searchQuery.data.documents.length})</p>
              <ul className="mt-1 space-y-1">
                {searchQuery.data.documents.map((d) => (
                  <li key={d.id} className="text-slate-700">
                    {d.title}
                  </li>
                ))}
              </ul>
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500">댓글 ({searchQuery.data.comments.length})</p>
              <ul className="mt-1 space-y-1">
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

          <div className="mt-4 rounded border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between text-sm">
              <span className="font-semibold text-slate-700">진행률</span>
              <span className="text-slate-500">{stats.progressRate}%</span>
            </div>
            <div className="mt-2 h-2 w-full rounded-full bg-slate-100">
              <div className="h-2 rounded-full bg-blue-500" style={{ width: `${stats.progressRate}%` }} />
            </div>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="rounded border border-slate-200 bg-white p-4">
              <h2 className="text-sm font-semibold text-slate-700">마감 임박 Task</h2>
              <ul className="mt-2 space-y-1 text-sm">
                {stats.dueSoonTasks.map((t) => (
                  <li key={t.id} className="flex justify-between">
                    <Link to={`/projects/${id}/board?taskId=${t.id}`} className="text-blue-600 hover:underline">
                      {t.title}
                    </Link>
                    <span className="text-slate-400">{t.dueDate}</span>
                  </li>
                ))}
                {stats.dueSoonTasks.length === 0 && <li className="text-slate-400">마감 임박 Task가 없습니다.</li>}
              </ul>
            </div>

            <div className="rounded border border-slate-200 bg-white p-4">
              <h2 className="text-sm font-semibold text-slate-700">최근 활동</h2>
              <ul className="mt-2 space-y-1 text-sm">
                {stats.recentActivities.map((a) => (
                  <li key={a.id} className="text-slate-600">
                    {a.description}
                  </li>
                ))}
                {stats.recentActivities.length === 0 && <li className="text-slate-400">최근 활동이 없습니다.</li>}
              </ul>
            </div>
          </div>
        </>
      )}
    </main>
  )
}
