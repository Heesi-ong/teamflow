import { http } from './http'
import type { Task } from './taskApi'
import type { DocumentSummary } from './documentApi'
import type { TaskComment } from './commentApi'

export interface ActivityLogItem {
  id: number
  actorId: number
  actorName: string
  actionType: string
  description: string
  createdAt: string
}

export interface DashboardStats {
  totalTasks: number
  doneTasks: number
  inProgressTasks: number
  todoTasks: number
  progressRate: number
  dueSoonTasks: Task[]
  memberCount: number
  recentActivities: ActivityLogItem[]
}

export interface SearchResults {
  tasks: Task[]
  documents: DocumentSummary[]
  comments: TaskComment[]
}

export const dashboardApi = {
  get: (projectId: number) => http.get<DashboardStats>(`/projects/${projectId}/dashboard`).then((res) => res.data),
  search: (projectId: number, keyword: string, type?: 'TASK' | 'DOCUMENT' | 'COMMENT') =>
    http.get<SearchResults>(`/projects/${projectId}/search`, { params: { keyword, type } }).then((res) => res.data),
}
