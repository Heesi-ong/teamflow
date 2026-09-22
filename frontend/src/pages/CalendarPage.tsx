import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { taskApi, type Task } from '../services/taskApi'

function pad(n: number) {
  return String(n).padStart(2, '0')
}

export function CalendarPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const [cursor, setCursor] = useState(() => new Date())

  const tasksQuery = useQuery({ queryKey: ['tasks', id], queryFn: () => taskApi.list(id) })

  const year = cursor.getFullYear()
  const month = cursor.getMonth() // 0-indexed

  const tasksByDate = useMemo(() => {
    const map = new Map<string, Task[]>()
    for (const task of tasksQuery.data?.content ?? []) {
      if (!task.dueDate) continue
      const list = map.get(task.dueDate) ?? []
      list.push(task)
      map.set(task.dueDate, list)
    }
    return map
  }, [tasksQuery.data])

  const firstDayOfMonth = new Date(year, month, 1)
  const startWeekday = firstDayOfMonth.getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const cells: (number | null)[] = [...Array(startWeekday).fill(null), ...Array.from({ length: daysInMonth }, (_, i) => i + 1)]

  return (
    <main className="min-h-screen bg-slate-50 p-6">
      <Link to={`/projects/${id}`} className="text-sm text-blue-600 underline">
        ← 프로젝트로
      </Link>
      <div className="mt-2 flex items-center justify-between">
        <h1 className="text-xl font-bold text-slate-800">
          {year}년 {month + 1}월
        </h1>
        <div className="flex gap-2">
          <button
            onClick={() => setCursor(new Date(year, month - 1, 1))}
            className="rounded bg-slate-200 px-2 py-1 text-sm"
          >
            ◀
          </button>
          <button onClick={() => setCursor(new Date())} className="rounded bg-slate-200 px-2 py-1 text-sm">
            오늘
          </button>
          <button
            onClick={() => setCursor(new Date(year, month + 1, 1))}
            className="rounded bg-slate-200 px-2 py-1 text-sm"
          >
            ▶
          </button>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-7 gap-1 text-center text-xs font-semibold text-slate-500">
        {['일', '월', '화', '수', '목', '금', '토'].map((d) => (
          <div key={d}>{d}</div>
        ))}
      </div>
      <div className="mt-1 grid grid-cols-7 gap-1">
        {cells.map((day, i) => {
          const dateKey = day ? `${year}-${pad(month + 1)}-${pad(day)}` : null
          const dayTasks = dateKey ? (tasksByDate.get(dateKey) ?? []) : []
          return (
            <div key={i} className="min-h-[90px] rounded border border-slate-200 bg-white p-1 text-left">
              {day && (
                <>
                  <p className="text-xs text-slate-400">{day}</p>
                  <ul className="mt-1 space-y-0.5">
                    {dayTasks.map((t) => (
                      <li key={t.id}>
                        <Link
                          to={`/projects/${id}/board?taskId=${t.id}`}
                          className="block truncate rounded bg-blue-50 px-1 text-xs text-blue-700 hover:bg-blue-100"
                        >
                          {t.title}
                        </Link>
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </div>
          )
        })}
      </div>
    </main>
  )
}
