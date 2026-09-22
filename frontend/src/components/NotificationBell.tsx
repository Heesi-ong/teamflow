import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { API_BASE_URL } from '../config'
import { notificationApi, type Notification } from '../services/notificationApi'
import { useAuthStore } from '../store/authStore'

export function NotificationBell() {
  const accessToken = useAuthStore((s) => s.accessToken)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)

  const notificationsQuery = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationApi.list(),
    enabled: !!accessToken,
  })
  const unreadCountQuery = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: notificationApi.unreadCount,
    enabled: !!accessToken,
  })

  // 10-realtime-architecture.md §1.1: EventSource는 커스텀 헤더를 지원하지 않아 Access Token을
  // 쿼리 파라미터로 전달한다. accessToken이 바뀌면(재발급 등) 연결을 다시 맺는다.
  useEffect(() => {
    if (!accessToken) return
    const source = new EventSource(`${API_BASE_URL}/api/notifications/subscribe?token=${accessToken}`)
    source.addEventListener('notification', () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    })
    return () => source.close()
  }, [accessToken, queryClient])

  const markReadMutation = useMutation({
    mutationFn: (id: number) => notificationApi.markRead(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  const markAllReadMutation = useMutation({
    mutationFn: () => notificationApi.markAllRead(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  if (!accessToken) return null

  const notifications = notificationsQuery.data?.content ?? []
  const unreadCount = unreadCountQuery.data ?? 0

  function handleClick(n: Notification) {
    if (!n.isRead) markReadMutation.mutate(n.id)
    setOpen(false)
    if (n.targetUrl) navigate(n.targetUrl)
  }

  return (
    <div className="fixed right-4 top-4 z-40">
      <button
        onClick={() => setOpen((o) => !o)}
        className="relative rounded-full bg-white p-2 shadow border border-slate-200"
        aria-label="알림"
      >
        🔔
        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 rounded-full bg-red-500 px-1.5 text-xs text-white">
            {unreadCount}
          </span>
        )}
      </button>
      {open && (
        <div className="mt-2 w-80 rounded border border-slate-200 bg-white shadow-lg">
          <div className="flex items-center justify-between border-b border-slate-100 px-3 py-2">
            <span className="text-sm font-semibold text-slate-700">알림</span>
            <button onClick={() => markAllReadMutation.mutate()} className="text-xs text-blue-600 hover:underline">
              모두 읽음
            </button>
          </div>
          <ul className="max-h-96 overflow-y-auto">
            {notifications.length === 0 && <li className="p-3 text-sm text-slate-400">알림이 없습니다.</li>}
            {notifications.map((n) => (
              <li key={n.id}>
                <button
                  onClick={() => handleClick(n)}
                  className={`block w-full px-3 py-2 text-left text-sm hover:bg-slate-50 ${n.isRead ? 'text-slate-400' : 'text-slate-800'}`}
                >
                  {n.message}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
