import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { gsap } from 'gsap'
import { useEffect, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { TaskDetailModal } from '../components/TaskDetailModal'
import { PRIORITY_BADGE, STATUS_ACCENT } from '../lib/taskVisuals'
import { projectApi } from '../services/projectApi'
import { STATUS_LABELS, TASK_STATUSES, taskApi, type Task, type TaskStatus } from '../services/taskApi'

export function KanbanBoardPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  // 알림의 targetUrl(예: /projects/1/board?taskId=5)로 들어오면 해당 Task 모달을 바로 연다.
  const [openTaskId, setOpenTaskId] = useState<number | null>(
    searchParams.get('taskId') ? Number(searchParams.get('taskId')) : null,
  )
  const [showCreate, setShowCreate] = useState(false)
  const [title, setTitle] = useState('')
  const boardRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const ctx = gsap.context(() => {
      gsap.from('[data-anim="column"]', { opacity: 0, y: 16, duration: 0.4, stagger: 0.08, ease: 'power2.out' })
    }, boardRef)
    return () => ctx.revert()
  }, [])

  function closeTaskModal() {
    setOpenTaskId(null)
    if (searchParams.has('taskId')) {
      searchParams.delete('taskId')
      setSearchParams(searchParams, { replace: true })
    }
  }

  const tasksQuery = useQuery({ queryKey: ['tasks', id], queryFn: () => taskApi.list(id) })
  const membersQuery = useQuery({ queryKey: ['members', id], queryFn: () => projectApi.members(id) })

  const createMutation = useMutation({
    mutationFn: () => taskApi.create(id, { title, description: '', assigneeId: null, priority: 'MEDIUM' }),
    onSuccess: () => {
      setTitle('')
      setShowCreate(false)
      queryClient.invalidateQueries({ queryKey: ['tasks', id] })
    },
  })

  const statusMutation = useMutation({
    mutationFn: (vars: { task: Task; status: TaskStatus }) =>
      taskApi.updateStatus(id, vars.task.id, vars.status, vars.task.version),
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['tasks', id] }),
  })

  function handleDrop(e: React.DragEvent, status: TaskStatus) {
    e.preventDefault()
    const taskId = Number(e.dataTransfer.getData('text/plain'))
    const task = tasksQuery.data?.content.find((t) => t.id === taskId)
    if (task && task.status !== status) {
      statusMutation.mutate({ task, status })
    }
  }

  const membersById = new Map((membersQuery.data ?? []).map((m) => [m.userId, m]))

  return (
    <main className="min-h-screen bg-slate-50 p-6">
      <div className="mb-5 flex items-center justify-between pr-14">
        <Link to={`/projects/${id}`} className="text-sm font-medium text-slate-500 hover:text-slate-700">
          ← 프로젝트로
        </Link>
        <button
          onClick={() => setShowCreate(true)}
          className="rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700"
        >
          + Task
        </button>
      </div>

      {showCreate && (
        <form
          onSubmit={(e) => {
            e.preventDefault()
            if (title.trim()) createMutation.mutate()
          }}
          className="mb-5 flex gap-2"
        >
          <input
            autoFocus
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Task 제목"
            className="flex-1 rounded-lg border border-slate-300 px-3.5 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100"
          />
          <button type="submit" className="rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-semibold text-white">
            생성
          </button>
          <button
            type="button"
            onClick={() => setShowCreate(false)}
            className="rounded-lg border border-slate-200 px-3.5 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50"
          >
            취소
          </button>
        </form>
      )}

      <div ref={boardRef} className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {TASK_STATUSES.map((status) => {
          const tasksInColumn = tasksQuery.data?.content.filter((task) => task.status === status) ?? []
          return (
            <div
              key={status}
              data-anim="column"
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => handleDrop(e, status)}
              className="min-h-[300px] rounded-xl border border-slate-200 bg-slate-100/70 p-3"
            >
              <h2 className="mb-3 flex items-center gap-2 px-1 text-xs font-semibold uppercase tracking-wide text-slate-500">
                <span className={`h-2 w-2 rounded-full ${STATUS_ACCENT[status]}`} />
                {STATUS_LABELS[status]}
                <span className="ml-auto rounded-full bg-white px-1.5 py-0.5 text-[11px] font-medium text-slate-400">
                  {tasksInColumn.length}
                </span>
              </h2>
              <div className="space-y-2">
                {tasksInColumn.map((task) => {
                  const assignee = task.assigneeId ? membersById.get(task.assigneeId) : undefined
                  return (
                    <div
                      key={task.id}
                      draggable
                      onDragStart={(e) => e.dataTransfer.setData('text/plain', String(task.id))}
                      onClick={() => setOpenTaskId(task.id)}
                      className="cursor-pointer rounded-lg border border-slate-200 bg-white p-3 shadow-sm transition hover:-translate-y-0.5 hover:border-primary-300 hover:shadow-md"
                    >
                      <p className="text-sm font-medium text-slate-800">{task.title}</p>
                      <div className="mt-2 flex items-center justify-between">
                        <span className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${PRIORITY_BADGE[task.priority]}`}>
                          {task.priority}
                        </span>
                        {assignee && (
                          <span
                            title={assignee.userName}
                            className="flex h-5 w-5 items-center justify-center rounded-full bg-primary-100 text-[10px] font-semibold text-primary-700"
                          >
                            {assignee.userName.slice(0, 1)}
                          </span>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          )
        })}
      </div>

      {openTaskId && (
        <TaskDetailModal projectId={id} taskId={openTaskId} members={membersQuery.data ?? []} onClose={closeTaskModal} />
      )}
    </main>
  )
}
