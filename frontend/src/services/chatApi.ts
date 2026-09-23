import { http } from './http'

export interface ChatMessage {
  id: number
  projectId: number
  authorId: number
  authorName: string
  content: string
  createdAt: string
}

export const chatApi = {
  history: (projectId: number, before?: number, size?: number) =>
    http.get<ChatMessage[]>(`/projects/${projectId}/chat/messages`, { params: { before, size } }).then((res) => res.data),
}
