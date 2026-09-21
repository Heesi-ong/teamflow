import { http } from './http'
import type { PageResponse } from './projectApi'

export interface DocumentSummary {
  id: number
  projectId: number
  authorId: number
  authorName: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface ProjectDocument extends DocumentSummary {
  content: string | null
}

export const documentApi = {
  list: (projectId: number) =>
    http.get<PageResponse<DocumentSummary>>(`/projects/${projectId}/documents`, { params: { size: 50 } }).then((res) => res.data),
  create: (projectId: number, body: { title: string; content: string }) =>
    http.post<ProjectDocument>(`/projects/${projectId}/documents`, body).then((res) => res.data),
  get: (projectId: number, documentId: number) =>
    http.get<ProjectDocument>(`/projects/${projectId}/documents/${documentId}`).then((res) => res.data),
  update: (projectId: number, documentId: number, body: { title?: string; content?: string }) =>
    http.patch<ProjectDocument>(`/projects/${projectId}/documents/${documentId}`, body).then((res) => res.data),
  remove: (projectId: number, documentId: number) => http.delete(`/projects/${projectId}/documents/${documentId}`),
}
