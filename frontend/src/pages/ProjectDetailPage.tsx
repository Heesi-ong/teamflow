import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { projectApi } from '../services/projectApi'

export function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const { data, isLoading, isError } = useQuery({
    queryKey: ['project', id],
    queryFn: () => projectApi.get(id),
  })

  return (
    <main className="mx-auto min-h-screen max-w-2xl bg-slate-50 p-6">
      <Link to="/projects" className="text-sm text-blue-600 underline">
        ← 프로젝트 목록
      </Link>
      {isLoading && <p className="mt-4 text-slate-500">불러오는 중...</p>}
      {isError && <p className="mt-4 text-red-500">프로젝트를 찾을 수 없거나 접근 권한이 없습니다.</p>}
      {data && (
        <div className="mt-4 rounded border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-bold text-slate-800">{data.name}</h1>
            <span className="rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-600">{data.status}</span>
          </div>
          <p className="mt-2 text-slate-600">{data.description || '설명 없음'}</p>
          <p className="mt-2 text-sm text-slate-400">
            {data.startDate ?? '?'} ~ {data.endDate ?? '?'}
          </p>
          <div className="mt-4 flex gap-2">
            <Link to={`/projects/${data.id}/board`} className="inline-block rounded bg-blue-600 px-3 py-2 text-white">
              Kanban Board
            </Link>
            <Link to={`/projects/${data.id}/members`} className="inline-block rounded bg-slate-600 px-3 py-2 text-white">
              팀원 관리
            </Link>
          </div>
        </div>
      )}
    </main>
  )
}
