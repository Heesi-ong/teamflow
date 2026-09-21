import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { TaskDetailModal } from '../components/TaskDetailModal'
import { projectApi } from '../services/projectApi'
import { STATUS_LABELS, TASK_STATUSES, taskApi, type Task, type TaskStatus } from '../services/taskApi'

export function KanbanBoardPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const queryClient = useQueryClient()
  const [openTaskId, setOpenTaskId] = useState<number | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [title, setTitle] = useState('')

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
      <div className="mb-4 flex items-center justify-between">
        <Link to={`/projects/${id}`} className="text-sm text-blue-600 underline">
          ← 프로젝트로
        </Link>
        <button onClick={() => setShowCreate(true)} className="rounded bg-blue-600 px-3 py-2 text-white">
          + Task
        </button>
      </div>

      {showCreate && (
        <form
          onSubmit={(e) => {
            e.preventDefault()
            if (title.trim()) createMutation.mutate()
          }}
          className="mb-4 flex gap-2"
        >
          <input
            autoFocus
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Task 제목"
            className="flex-1 rounded border border-slate-300 px-3 py-2"
          />
          <button type="submit" className="rounded bg-blue-600 px-3 py-2 text-white">
            생성
          </button>
          <button type="button" onClick={() => setShowCreate(false)} className="rounded bg-slate-200 px-3 py-2">
            취소
          </button>
        </form>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {TASK_STATUSES.map((status) => (
          <div
            key={status}
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => handleDrop(e, status)}
            className="min-h-[300px] rounded border border-slate-200 bg-slate-100 p-3"
          >
            <h2 className="mb-2 text-sm font-semibold text-slate-600">{STATUS_LABELS[status]}</h2>
            <div className="space-y-2">
              {tasksQuery.data?.content
                .filter((task) => task.status === status)
                .map((task) => (
                  <div
                    key={task.id}
                    draggable
                    onDragStart={(e) => e.dataTransfer.setData('text/plain', String(task.id))}
                    onClick={() => setOpenTaskId(task.id)}
                    className="cursor-pointer rounded border border-slate-200 bg-white p-3 shadow-sm hover:border-blue-400"
                  >
                    <p className="text-sm font-medium text-slate-800">{task.title}</p>
                    <div className="mt-1 flex items-center justify-between text-xs text-slate-400">
                      <span>{task.priority}</span>
                      {task.assigneeId && <span>{membersById.get(task.assigneeId)?.userName ?? '담당자'}</span>}
                    </div>
                  </div>
                ))}
            </div>
          </div>
        ))}
      </div>

      {openTaskId && (
        <TaskDetailModal
          projectId={id}
          taskId={openTaskId}
          members={membersQuery.data ?? []}
          onClose={() => setOpenTaskId(null)}
        />
      )}
    </main>
  )
}
