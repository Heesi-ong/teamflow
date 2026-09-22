import { http } from './http'
import type { PageResponse } from './projectApi'

export type NotificationType =
  | 'TASK_ASSIGNED'
  | 'TASK_STATUS_CHANGED'
  | 'MENTION'
  | 'PROJECT_INVITE'
  | 'COMMENT_ADDED'
  | 'DUE_SOON'
  | 'ANNOUNCEMENT'

export interface Notification {
  id: number
  type: NotificationType
  message: string
  targetUrl: string | null
  isRead: boolean
  createdAt: string
}

export const notificationApi = {
  list: (isRead?: boolean) =>
    http.get<PageResponse<Notification>>('/notifications', { params: { isRead, size: 20 } }).then((res) => res.data),
  markRead: (id: number) => http.patch<Notification>(`/notifications/${id}/read`).then((res) => res.data),
  markAllRead: () => http.patch('/notifications/read-all'),
  unreadCount: () => http.get<{ unreadCount: number }>('/notifications/unread-count').then((res) => res.data.unreadCount),
}
