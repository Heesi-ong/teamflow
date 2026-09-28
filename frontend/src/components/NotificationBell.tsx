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

  // EventSource는 커스텀 Authorization 헤더를 지원하지 않으므로 fetch 스트림으로
  // 인증 헤더를 직접 전달한다. 토큰을 URL query parameter에 넣지 않아 서버 로그나
  // 프록시 기록으로 토큰이 유출될 가능성을 줄인다.
  useEffect(() => {
    if (!accessToken) return
    let stopped = false
    let retryTimer: number | undefined
    let activeController: AbortController | null = null

    async function connect() {
      if (stopped) return
      const controller = new AbortController()
      activeController = controller
      try {
        const response = await fetch(`${API_BASE_URL}/api/notifications/subscribe`, {
          headers: {
            Accept: 'text/event-stream',
            Authorization: `Bearer ${accessToken}`,
          },
          credentials: 'include',
          signal: controller.signal,
        })
        if (!response.ok || !response.body) throw new Error(`SSE connection failed: ${response.status}`)

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        while (!stopped) {
          const { value, done } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          const events = buffer.split(/\r?\n\r?\n/)
          buffer = events.pop() ?? ''
          // SSE 스펙은 "field:value"의 콜론 뒤 공백 하나를 허용만 할 뿐 요구하지 않는다 — Spring의
          // SseEmitter는 공백 없이 "event:notification"으로 보낸다. 정규식으로 둘 다 받아들인다.
          if (events.some((event) => event.split(/\r?\n/).some((line) => /^event:\s?notification$/.test(line)))) {
            queryClient.invalidateQueries({ queryKey: ['notifications'] })
            queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] })
          }
        }
      } catch {
        // Abort is expected during route changes or logout. Other failures retry below.
      } finally {
        if (activeController === controller) activeController = null
        if (!stopped) retryTimer = window.setTimeout(() => void connect(), 2000)
      }
    }

    void connect()
    return () => {
      stopped = true
      if (retryTimer !== undefined) window.clearTimeout(retryTimer)
      activeController?.abort()
    }
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
