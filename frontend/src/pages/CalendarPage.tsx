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
  const todayKey = `${new Date().getFullYear()}-${pad(new Date().getMonth() + 1)}-${pad(new Date().getDate())}`

  return (
    <main className="min-h-screen bg-slate-50 p-6">
      <Link to={`/projects/${id}`} className="text-sm font-medium text-slate-500 hover:text-slate-700">
        ← 프로젝트로
      </Link>
      <div className="mt-2 flex items-center justify-between">
        <h1 className="text-xl font-bold text-slate-900">
          {year}년 {month + 1}월
        </h1>
        <div className="flex gap-1.5">
          <button
            onClick={() => setCursor(new Date(year, month - 1, 1))}
            className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-sm text-slate-600 hover:bg-slate-100"
          >
            ◀
          </button>
          <button
            onClick={() => setCursor(new Date())}
            className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100"
          >
            오늘
          </button>
          <button
            onClick={() => setCursor(new Date(year, month + 1, 1))}
            className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-sm text-slate-600 hover:bg-slate-100"
          >
            ▶
          </button>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-7 gap-1.5 text-center text-xs font-semibold text-slate-500">
        {['일', '월', '화', '수', '목', '금', '토'].map((d) => (
          <div key={d}>{d}</div>
        ))}
      </div>
      <div className="mt-1.5 grid grid-cols-7 gap-1.5">
        {cells.map((day, i) => {
          const dateKey = day ? `${year}-${pad(month + 1)}-${pad(day)}` : null
          const dayTasks = dateKey ? (tasksByDate.get(dateKey) ?? []) : []
          const isToday = dateKey === todayKey
          return (
            <div
              key={i}
              className={`min-h-[90px] rounded-lg border p-1.5 text-left ${
                isToday ? 'border-primary-300 bg-primary-50/40' : 'border-slate-200 bg-white'
              }`}
            >
              {day && (
                <>
                  <p className={`text-xs ${isToday ? 'font-semibold text-primary-600' : 'text-slate-400'}`}>{day}</p>
                  <ul className="mt-1 space-y-0.5">
                    {dayTasks.map((t) => (
                      <li key={t.id}>
                        <Link
                          to={`/projects/${id}/board?taskId=${t.id}`}
                          className="block truncate rounded bg-primary-50 px-1 text-xs text-primary-700 hover:bg-primary-100"
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
