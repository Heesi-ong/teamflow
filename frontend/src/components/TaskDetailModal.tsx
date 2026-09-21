import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { ProjectMember } from '../services/projectApi'
import { taskApi, type TaskPriority } from '../services/taskApi'

const PRIORITIES: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

export function TaskDetailModal({
  projectId,
  taskId,
  members,
  onClose,
}: {
  projectId: number
  taskId: number
  members: ProjectMember[]
  onClose: () => void
}) {
  const queryClient = useQueryClient()
  const [checklistInput, setChecklistInput] = useState('')
  const [conflict, setConflict] = useState(false)

  const taskQuery = useQuery({
    queryKey: ['task', projectId, taskId],
    queryFn: () => taskApi.get(projectId, taskId),
  })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['task', projectId, taskId] })
    queryClient.invalidateQueries({ queryKey: ['tasks', projectId] })
  }

  function handleConflict(err: any) {
    if (err.response?.status === 409) {
      setConflict(true)
      invalidate()
    }
  }

  const updateFieldsMutation = useMutation({
    mutationFn: (body: { title?: string; description?: string; priority?: TaskPriority }) =>
      taskApi.update(projectId, taskId, { ...body, version: taskQuery.data!.version }),
    onSuccess: invalidate,
    onError: handleConflict,
  })

  const updateAssigneeMutation = useMutation({
    mutationFn: (assigneeId: number) => taskApi.updateAssignee(projectId, taskId, assigneeId, taskQuery.data!.version),
    onSuccess: invalidate,
    onError: handleConflict,
  })

  const updateStatusMutation = useMutation({
    mutationFn: (status: Parameters<typeof taskApi.updateStatus>[2]) =>
      taskApi.updateStatus(projectId, taskId, status, taskQuery.data!.version),
    onSuccess: invalidate,
    onError: handleConflict,
  })

  const deleteMutation = useMutation({
    mutationFn: () => taskApi.remove(projectId, taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', projectId] })
      onClose()
    },
  })

  const addChecklistMutation = useMutation({
    mutationFn: (content: string) => taskApi.addChecklist(taskId, content),
    onSuccess: () => {
      setChecklistInput('')
      invalidate()
    },
  })

  const toggleChecklistMutation = useMutation({
    mutationFn: (vars: { checklistId: number; isDone: boolean }) =>
      taskApi.updateChecklist(taskId, vars.checklistId, { isDone: vars.isDone }),
    onSuccess: invalidate,
  })

  const removeChecklistMutation = useMutation({
    mutationFn: (checklistId: number) => taskApi.removeChecklist(taskId, checklistId),
    onSuccess: invalidate,
  })

  const task = taskQuery.data

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded bg-white p-6 shadow-lg"
        onClick={(e) => e.stopPropagation()}
      >
        {taskQuery.isLoading && <p className="text-slate-500">불러오는 중...</p>}
        {task && (
          <>
            <div className="flex items-start justify-between">
              <input
                defaultValue={task.title}
                key={`title-${task.version}`}
                onBlur={(e) => e.target.value !== task.title && updateFieldsMutation.mutate({ title: e.target.value })}
                className="w-full rounded border border-transparent px-1 text-lg font-semibold hover:border-slate-200 focus:border-slate-300"
              />
              <button onClick={onClose} className="ml-2 text-slate-400 hover:text-slate-600">
                ✕
              </button>
            </div>
            {conflict && (
              <p className="mt-1 text-sm text-red-500">
                다른 사용자가 먼저 이 Task를 수정했습니다. 최신 내용으로 갱신했습니다 — 변경 사항을 다시 반영해주세요.
              </p>
            )}
            <textarea
              defaultValue={task.description ?? ''}
              key={`desc-${task.version}`}
              placeholder="설명 없음"
              onBlur={(e) => e.target.value !== (task.description ?? '') && updateFieldsMutation.mutate({ description: e.target.value })}
              className="mt-2 w-full rounded border border-transparent px-1 text-sm text-slate-600 hover:border-slate-200 focus:border-slate-300"
              rows={3}
            />

            <div className="mt-3 grid grid-cols-3 gap-2 text-sm">
              <label className="flex flex-col gap-1">
                상태
                <select
                  value={task.status}
                  onChange={(e) => updateStatusMutation.mutate(e.target.value as any)}
                  className="rounded border border-slate-300 px-2 py-1"
                >
                  {['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'].map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </label>
              <label className="flex flex-col gap-1">
                우선순위
                <select
                  value={task.priority}
                  onChange={(e) => updateFieldsMutation.mutate({ priority: e.target.value as TaskPriority })}
                  className="rounded border border-slate-300 px-2 py-1"
                >
                  {PRIORITIES.map((p) => (
                    <option key={p} value={p}>
                      {p}
                    </option>
                  ))}
                </select>
              </label>
              <label className="flex flex-col gap-1">
                담당자
                <select
                  value={task.assigneeId ?? ''}
                  onChange={(e) => e.target.value && updateAssigneeMutation.mutate(Number(e.target.value))}
                  className="rounded border border-slate-300 px-2 py-1"
                >
                  <option value="">미지정</option>
                  {members.map((m) => (
                    <option key={m.userId} value={m.userId}>
                      {m.userName}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className="mt-4">
              <h3 className="text-sm font-semibold text-slate-700">Checklist</h3>
              <ul className="mt-1 space-y-1">
                {task.checklists.map((item) => (
                  <li key={item.id} className="flex items-center gap-2 text-sm">
                    <input
                      type="checkbox"
                      checked={item.isDone}
                      onChange={(e) => toggleChecklistMutation.mutate({ checklistId: item.id, isDone: e.target.checked })}
                    />
                    <span className={item.isDone ? 'flex-1 text-slate-400 line-through' : 'flex-1 text-slate-700'}>
                      {item.content}
                    </span>
                    <button
                      onClick={() => removeChecklistMutation.mutate(item.id)}
                      className="text-xs text-red-400 hover:underline"
                    >
                      삭제
                    </button>
                  </li>
                ))}
              </ul>
              <form
                onSubmit={(e) => {
                  e.preventDefault()
                  if (checklistInput.trim()) addChecklistMutation.mutate(checklistInput.trim())
                }}
                className="mt-2 flex gap-2"
              >
                <input
                  value={checklistInput}
                  onChange={(e) => setChecklistInput(e.target.value)}
                  placeholder="체크리스트 항목 추가"
                  className="flex-1 rounded border border-slate-300 px-2 py-1 text-sm"
                />
                <button type="submit" className="rounded bg-slate-600 px-2 py-1 text-sm text-white">
                  추가
                </button>
              </form>
            </div>

            <button
              onClick={() => deleteMutation.mutate()}
              className="mt-6 text-sm text-red-500 hover:underline"
            >
              Task 삭제
            </button>
          </>
        )}
      </div>
    </div>
  )
}
