import type { TaskPriority, TaskStatus } from '../services/taskApi'

// Kanban Board 카드/컬럼에서 재사용하는 우선순위·상태 색상 토큰.
export const PRIORITY_BADGE: Record<TaskPriority, string> = {
  LOW: 'bg-slate-100 text-slate-600',
  MEDIUM: 'bg-blue-50 text-blue-600',
  HIGH: 'bg-amber-50 text-amber-700',
  URGENT: 'bg-red-50 text-red-600',
}

export const STATUS_ACCENT: Record<TaskStatus, string> = {
  TODO: 'bg-slate-300',
  IN_PROGRESS: 'bg-primary-500',
  REVIEW: 'bg-amber-400',
  DONE: 'bg-emerald-500',
}
