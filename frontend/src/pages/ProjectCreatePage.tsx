import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { projectApi } from '../services/projectApi'

export function ProjectCreatePage() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const project = await projectApi.create({
        name,
        description,
        startDate: startDate || null,
        endDate: endDate || null,
      })
      navigate(`/projects/${project.id}`)
    } catch (err: any) {
      setError(err.response?.data?.message ?? '프로젝트 생성에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-slate-50">
      <h1 className="text-2xl font-bold text-blue-600">새 프로젝트</h1>
      <form onSubmit={handleSubmit} className="flex w-80 flex-col gap-3">
        <input
          type="text"
          placeholder="프로젝트명"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          className="rounded border border-slate-300 px-3 py-2"
        />
        <textarea
          placeholder="설명"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="rounded border border-slate-300 px-3 py-2"
        />
        <label className="text-sm text-slate-500">
          시작일
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
          />
        </label>
        <label className="text-sm text-slate-500">
          종료일
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
          />
        </label>
        {error && <p className="text-sm text-red-500">{error}</p>}
        <button
          type="submit"
          disabled={submitting}
          className="rounded bg-blue-600 px-3 py-2 text-white disabled:opacity-50"
        >
          생성
        </button>
      </form>
    </main>
  )
}
