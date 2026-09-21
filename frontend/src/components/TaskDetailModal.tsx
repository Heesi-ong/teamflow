import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { commentApi } from '../services/commentApi'
import { fileApi } from '../services/fileApi'
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
  const [commentInput, setCommentInput] = useState('')
  const [conflict, setConflict] = useState(false)

  const taskQuery = useQuery({
    queryKey: ['task', projectId, taskId],
    queryFn: () => taskApi.get(projectId, taskId),
  })

  const commentsQuery = useQuery({
    queryKey: ['comments', taskId],
    queryFn: () => commentApi.list(taskId),
  })

  const filesQuery = useQuery({
    queryKey: ['task-files', projectId, taskId],
    queryFn: () => fileApi.list(projectId, taskId),
  })
  const fileInputRef = useRef<HTMLInputElement>(null)

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

  const addCommentMutation = useMutation({
    mutationFn: (content: string) => commentApi.create(taskId, content),
    onSuccess: () => {
      setCommentInput('')
      queryClient.invalidateQueries({ queryKey: ['comments', taskId] })
    },
  })

  const removeCommentMutation = useMutation({
    mutationFn: (commentId: number) => commentApi.remove(taskId, commentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['comments', taskId] }),
  })

  const uploadFileMutation = useMutation({
    mutationFn: (file: File) => fileApi.upload(projectId, file, taskId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['task-files', projectId, taskId] }),
  })

  const removeFileMutation = useMutation({
    mutationFn: (fileId: number) => fileApi.remove(projectId, fileId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['task-files', projectId, taskId] }),
  })

  async function handleFileDownload(fileId: number) {
    const url = await fileApi.downloadUrl(projectId, fileId)
    window.open(url, '_blank')
  }

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

            <div className="mt-4">
              <h3 className="text-sm font-semibold text-slate-700">댓글</h3>
              <ul className="mt-1 space-y-2">
                {commentsQuery.data?.content.map((c) => (
                  <li key={c.id} className="rounded bg-slate-50 p-2 text-sm">
                    <div className="flex items-center justify-between">
                      <span className="font-medium text-slate-700">{c.authorName}</span>
                      <button
                        onClick={() => removeCommentMutation.mutate(c.id)}
                        className="text-xs text-red-400 hover:underline"
                      >
                        삭제
                      </button>
                    </div>
                    <p className="mt-1 text-slate-600">{c.content}</p>
                  </li>
                ))}
              </ul>
              <form
                onSubmit={(e) => {
                  e.preventDefault()
                  if (commentInput.trim()) addCommentMutation.mutate(commentInput.trim())
                }}
                className="mt-2 flex gap-2"
              >
                <input
                  value={commentInput}
                  onChange={(e) => setCommentInput(e.target.value)}
                  placeholder="댓글 작성 (@이름 으로 멘션)"
                  className="flex-1 rounded border border-slate-300 px-2 py-1 text-sm"
                />
                <button type="submit" className="rounded bg-slate-600 px-2 py-1 text-sm text-white">
                  작성
                </button>
              </form>
            </div>

            <div className="mt-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-slate-700">첨부 파일</h3>
                <button onClick={() => fileInputRef.current?.click()} className="text-xs text-blue-600 hover:underline">
                  + 파일 첨부
                </button>
                <input
                  ref={fileInputRef}
                  type="file"
                  className="hidden"
                  onChange={(e) => {
                    const file = e.target.files?.[0]
                    if (file) uploadFileMutation.mutate(file)
                    e.target.value = ''
                  }}
                />
              </div>
              {uploadFileMutation.isPending && <p className="mt-1 text-xs text-slate-400">업로드 중...</p>}
              <ul className="mt-1 space-y-1">
                {filesQuery.data?.content.map((f) => (
                  <li key={f.id} className="flex items-center justify-between text-sm">
                    <button onClick={() => handleFileDownload(f.id)} className="truncate text-blue-600 hover:underline">
                      {f.fileName}
                    </button>
                    <button onClick={() => removeFileMutation.mutate(f.id)} className="text-xs text-red-400 hover:underline">
                      삭제
                    </button>
                  </li>
                ))}
                {filesQuery.data?.content.length === 0 && <li className="text-sm text-slate-400">첨부된 파일이 없습니다.</li>}
              </ul>
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
