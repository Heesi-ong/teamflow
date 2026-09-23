import { http } from './http'
import type { PageResponse } from './projectApi'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export const TASK_STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE']
export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'TODO',
  IN_PROGRESS: 'IN PROGRESS',
  REVIEW: 'REVIEW',
  DONE: 'DONE',
}

export interface Task {
  id: number
  projectId: number
  title: string
  description: string | null
  authorId: number
  assigneeId: number | null
  status: TaskStatus
  priority: TaskPriority
  startDate: string | null
  dueDate: string | null
  version: number
  createdAt: string
  updatedAt: string
}

export interface TaskChecklistItem {
  id: number
  content: string
  isDone: boolean
  sortOrder: number
}

export interface TaskDetail extends Task {
  checklists: TaskChecklistItem[]
}

export const taskApi = {
  list: (projectId: number) =>
    http.get<PageResponse<Task>>(`/projects/${projectId}/tasks`, { params: { size: 100 } }).then((res) => res.data),
  create: (
    projectId: number,
    body: { title: string; description: string; assigneeId: number | null; priority: TaskPriority },
  ) => http.post<Task>(`/projects/${projectId}/tasks`, body).then((res) => res.data),
  get: (projectId: number, taskId: number) =>
    http.get<TaskDetail>(`/projects/${projectId}/tasks/${taskId}`).then((res) => res.data),
  update: (
    projectId: number,
    taskId: number,
    body: { title?: string; description?: string; priority?: TaskPriority; version: number },
  ) => http.patch<Task>(`/projects/${projectId}/tasks/${taskId}`, body).then((res) => res.data),
  updateStatus: (projectId: number, taskId: number, status: TaskStatus, version: number) =>
    http.patch<Task>(`/projects/${projectId}/tasks/${taskId}/status`, { status, version }).then((res) => res.data),
  updateAssignee: (projectId: number, taskId: number, assigneeId: number | null, version: number) =>
    http.patch<Task>(`/projects/${projectId}/tasks/${taskId}/assignee`, {
      assigneeId: assigneeId ?? undefined,
      clearAssignee: assigneeId === null,
      version,
    }).then((res) => res.data),
  remove: (projectId: number, taskId: number) => http.delete(`/projects/${projectId}/tasks/${taskId}`),
  addChecklist: (taskId: number, content: string) =>
    http.post<TaskChecklistItem>(`/tasks/${taskId}/checklists`, { content }).then((res) => res.data),
  updateChecklist: (taskId: number, checklistId: number, body: { content?: string; isDone?: boolean }) =>
    http.patch<TaskChecklistItem>(`/tasks/${taskId}/checklists/${checklistId}`, body).then((res) => res.data),
  removeChecklist: (taskId: number, checklistId: number) => http.delete(`/tasks/${taskId}/checklists/${checklistId}`),
}
