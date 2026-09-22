import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { projectApi } from '../services/projectApi'

export function ProjectListPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectApi.list(),
  })

  return (
    <main className="mx-auto min-h-screen max-w-3xl bg-slate-50 p-6">
      <div className="mb-5 flex items-center justify-between pr-14">
        <h1 className="text-xl font-bold text-slate-900">내 프로젝트</h1>
        <Link
          to="/projects/new"
          className="rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-semibold text-white shadow-sm hover:bg-primary-700"
        >
          새 프로젝트
        </Link>
      </div>
      {isLoading && <p className="text-sm text-slate-500">불러오는 중...</p>}
      {isError && <p className="text-sm text-red-500">프로젝트 목록을 불러오지 못했습니다.</p>}
      {data && data.content.length === 0 && <p className="text-sm text-slate-400">참여 중인 프로젝트가 없습니다.</p>}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {data?.content.map((project) => (
          <Link
            key={project.id}
            to={`/projects/${project.id}`}
            className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:border-primary-300 hover:shadow-md"
          >
            <h2 className="font-semibold text-slate-900">{project.name}</h2>
            <p className="mt-1 text-sm text-slate-500">{project.description || '설명 없음'}</p>
            <span className="mt-2 inline-block rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
              {project.status}
            </span>
          </Link>
        ))}
      </div>
    </main>
  )
}
