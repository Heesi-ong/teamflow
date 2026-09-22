import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { commentApi } from '../services/commentApi'
import { fileApi, MAX_FILE_SIZE_BYTES } from '../services/fileApi'
import { PRIORITY_BADGE } from '../lib/taskVisuals'
import type { ProjectMember } from '../services/projectApi'
import { taskApi, type TaskPriority } from '../services/taskApi'
import { FILE_STORAGE_ENABLED } from '../config'

const PRIORITIES: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

const selectClass =
  'rounded-lg border border-slate-300 px-2.5 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100'
const smallInputClass =
  'flex-1 rounded-lg border border-slate-300 px-2.5 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100'
const smallButtonClass = 'rounded-lg bg-slate-700 px-3 py-1.5 text-sm font-medium text-white hover:bg-slate-800'

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
  const [uploadFileError, setUploadFileError] = useState<string | null>(null)

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
    onSuccess: () => {
      setUploadFileError(null)
      queryClient.invalidateQueries({ queryKey: ['task-files', projectId, taskId] })
    },
    onError: (err: any) => setUploadFileError(err.response?.data?.message ?? err.message ?? '업로드에 실패했습니다.'),
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4" onClick={onClose}>
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        {taskQuery.isLoading && <p className="text-sm text-slate-500">불러오는 중...</p>}
        {task && (
          <>
            <div className="flex items-start justify-between gap-2">
              <input
                defaultValue={task.title}
                key={`title-${task.version}`}
                onBlur={(e) => e.target.value !== task.title && updateFieldsMutation.mutate({ title: e.target.value })}
                className="w-full rounded-lg border border-transparent px-1.5 py-0.5 text-lg font-semibold text-slate-900 hover:border-slate-200 focus:border-primary-400 focus:outline-none"
              />
              <button onClick={onClose} className="mt-1 shrink-0 text-slate-400 hover:text-slate-600">
                ✕
              </button>
            </div>
            {conflict && (
              <p className="mt-2 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-700">
                다른 사용자가 먼저 이 Task를 수정했습니다. 최신 내용으로 갱신했습니다 — 변경 사항을 다시 반영해주세요.
              </p>
            )}
            <textarea
              defaultValue={task.description ?? ''}
              key={`desc-${task.version}`}
              placeholder="설명 없음"
              onBlur={(e) => e.target.value !== (task.description ?? '') && updateFieldsMutation.mutate({ description: e.target.value })}
              className="mt-2 w-full rounded-lg border border-transparent px-1.5 py-1 text-sm text-slate-600 hover:border-slate-200 focus:border-primary-400 focus:outline-none"
              rows={3}
            />

            <div className="mt-3 flex items-center gap-2">
              <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${PRIORITY_BADGE[task.priority]}`}>
                {task.priority}
              </span>
            </div>

            <div className="mt-3 grid grid-cols-3 gap-2 text-sm">
              <label className="flex flex-col gap-1 text-xs font-medium text-slate-500">
                상태
                <select
                  value={task.status}
                  onChange={(e) => updateStatusMutation.mutate(e.target.value as any)}
                  className={selectClass}
                >
                  {['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'].map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </label>
              <label className="flex flex-col gap-1 text-xs font-medium text-slate-500">
                우선순위
                <select
                  value={task.priority}
                  onChange={(e) => updateFieldsMutation.mutate({ priority: e.target.value as TaskPriority })}
                  className={selectClass}
                >
                  {PRIORITIES.map((p) => (
                    <option key={p} value={p}>
                      {p}
                    </option>
                  ))}
                </select>
              </label>
              <label className="flex flex-col gap-1 text-xs font-medium text-slate-500">
                담당자
                <select
                  value={task.assigneeId ?? ''}
                  onChange={(e) => e.target.value && updateAssigneeMutation.mutate(Number(e.target.value))}
                  className={selectClass}
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

            <div className="mt-5">
              <h3 className="text-sm font-semibold text-slate-700">Checklist</h3>
              <ul className="mt-2 space-y-1.5">
                {task.checklists.map((item) => (
                  <li key={item.id} className="flex items-center gap-2 text-sm">
                    <input
                      type="checkbox"
                      checked={item.isDone}
                      onChange={(e) => toggleChecklistMutation.mutate({ checklistId: item.id, isDone: e.target.checked })}
                      className="h-4 w-4 rounded border-slate-300 text-primary-600 focus:ring-primary-400"
                    />
                    <span className={item.isDone ? 'flex-1 text-slate-400 line-through' : 'flex-1 text-slate-700'}>
                      {item.content}
                    </span>
                    <button
                      onClick={() => removeChecklistMutation.mutate(item.id)}
                      className="text-xs text-slate-400 hover:text-red-500"
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
                className="mt-2.5 flex gap-2"
              >
                <input
                  value={checklistInput}
                  onChange={(e) => setChecklistInput(e.target.value)}
                  placeholder="체크리스트 항목 추가"
                  className={smallInputClass}
                />
                <button type="submit" className={smallButtonClass}>
                  추가
                </button>
              </form>
            </div>

            <div className="mt-5">
              <h3 className="text-sm font-semibold text-slate-700">댓글</h3>
              <ul className="mt-2 space-y-2">
                {commentsQuery.data?.content.map((c) => (
                  <li key={c.id} className="rounded-lg bg-slate-50 p-2.5 text-sm">
                    <div className="flex items-center justify-between">
                      <span className="font-medium text-slate-700">{c.authorName}</span>
                      <button
                        onClick={() => removeCommentMutation.mutate(c.id)}
                        className="text-xs text-slate-400 hover:text-red-500"
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
                className="mt-2.5 flex gap-2"
              >
                <input
                  value={commentInput}
                  onChange={(e) => setCommentInput(e.target.value)}
                  placeholder="댓글 작성 (@이름 으로 멘션)"
                  className={smallInputClass}
                />
                <button type="submit" className={smallButtonClass}>
                  작성
                </button>
              </form>
            </div>

            <div className="mt-5">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-slate-700">첨부 파일</h3>
                {FILE_STORAGE_ENABLED && (
                  <button onClick={() => fileInputRef.current?.click()} className="text-xs font-medium text-primary-600 hover:text-primary-700">
                    + 파일 첨부
                  </button>
                )}
                <input
                  ref={fileInputRef}
                  type="file"
                  className="hidden"
                  onChange={(e) => {
                    const file = e.target.files?.[0]
                    if (file && file.size > MAX_FILE_SIZE_BYTES) {
                      setUploadFileError('파일은 최대 50MB까지 업로드할 수 있습니다.')
                    } else if (file) {
                      uploadFileMutation.mutate(file)
                    }
                    e.target.value = ''
                  }}
                />
              </div>
              {!FILE_STORAGE_ENABLED && <p className="mt-1 text-xs text-amber-600">현재 배포 환경에서는 파일 기능을 사용할 수 없습니다.</p>}
              {FILE_STORAGE_ENABLED && <p className="mt-1 text-xs text-slate-400">파일당 최대 50MB</p>}
              {uploadFileMutation.isPending && <p className="mt-1 text-xs text-slate-400">업로드 중...</p>}
              {uploadFileError && <p className="mt-1 text-xs text-red-500">{uploadFileError}</p>}
              <ul className="mt-2 space-y-1">
                {filesQuery.data?.content.map((f) => (
                  <li key={f.id} className="flex items-center justify-between text-sm">
                    <button disabled={!FILE_STORAGE_ENABLED} onClick={() => handleFileDownload(f.id)} className="truncate text-primary-600 hover:underline disabled:text-slate-400">
                      {f.fileName}
                    </button>
                    <button disabled={!FILE_STORAGE_ENABLED} onClick={() => removeFileMutation.mutate(f.id)} className="text-xs text-slate-400 hover:text-red-500 disabled:opacity-40">
                      삭제
                    </button>
                  </li>
                ))}
                {filesQuery.data?.content.length === 0 && <li className="text-sm text-slate-400">첨부된 파일이 없습니다.</li>}
              </ul>
            </div>

            <button onClick={() => deleteMutation.mutate()} className="mt-6 text-sm font-medium text-red-500 hover:text-red-600">
              Task 삭제
            </button>
          </>
        )}
      </div>
    </div>
  )
}
