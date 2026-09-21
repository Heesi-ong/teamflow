import { http } from './http'
import type { PageResponse } from './projectApi'

export interface TaskComment {
  id: number
  taskId: number
  authorId: number
  authorName: string
  content: string
  createdAt: string
  updatedAt: string
}

export const commentApi = {
  list: (taskId: number) =>
    http.get<PageResponse<TaskComment>>(`/tasks/${taskId}/comments`, { params: { size: 50 } }).then((res) => res.data),
  create: (taskId: number, content: string) =>
    http.post<TaskComment>(`/tasks/${taskId}/comments`, { content }).then((res) => res.data),
  remove: (taskId: number, commentId: number) => http.delete(`/tasks/${taskId}/comments/${commentId}`),
}
