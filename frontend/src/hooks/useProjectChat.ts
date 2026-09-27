import { Client } from '@stomp/stompjs'
import { useEffect, useRef, useState } from 'react'
import { WS_BASE_URL } from '../config'
import { chatApi, type ChatMessage } from '../services/chatApi'
import { useAuthStore } from '../store/authStore'

function mergeMessages(current: ChatMessage[], incoming: ChatMessage[]) {
  const byId = new Map(current.map((message) => [message.id, message]))
  incoming.forEach((message) => byId.set(message.id, message))
  return [...byId.values()].sort((left, right) => left.id - right.id)
}

/**
 * STOMP connection for one project's chat, shared by the full chat page and the
 * floating chat widget. `enabled` lets the floating widget defer connecting
 * until it's actually opened.
 */
export function useProjectChat(projectId: number, enabled: boolean) {
  const accessToken = useAuthStore((s) => s.accessToken)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [connected, setConnected] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const clientRef = useRef<Client | null>(null)

  useEffect(() => {
    if (!enabled) return

    let cancelled = false
    chatApi.history(projectId)
      .then((history) => {
        if (!cancelled) setMessages((previous) => mergeMessages([...history].reverse(), previous))
      })
      .catch(() => {
        if (!cancelled) setError('채팅 내역을 불러오지 못했습니다.')
      })

    if (!accessToken) return () => { cancelled = true }

    // 10-realtime-architecture.md §2.1: 인증은 STOMP CONNECT 프레임의 Authorization 헤더로 전달한다.
    const client = new Client({
      brokerURL: `${WS_BASE_URL}/ws/chat`,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 2000,
      onConnect: () => {
        if (clientRef.current !== client) return
        setConnected(true)
        setError(null)
        client.subscribe(`/topic/projects/${projectId}/chat`, (frame) => {
          const message: ChatMessage = JSON.parse(frame.body)
          setMessages((previous) => mergeMessages(previous, [message]))
        })
        client.subscribe('/user/queue/errors', (frame) => {
          const response = JSON.parse(frame.body) as { message?: string }
          setError(response.message ?? '메시지 전송에 실패했습니다.')
        })
      },
      onDisconnect: () => {
        if (clientRef.current === client) setConnected(false)
      },
      onStompError: () => {
        if (clientRef.current !== client) return
        setConnected(false)
        setError('채팅 연결에 실패했습니다.')
      },
    })

    clientRef.current = client
    client.activate()
    return () => {
      cancelled = true
      client.deactivate()
      if (clientRef.current === client) clientRef.current = null
      setConnected(false)
    }
  }, [accessToken, enabled, projectId])

  function sendMessage(content: string) {
    const trimmed = content.trim()
    if (!trimmed || !clientRef.current?.connected) return false
    clientRef.current.publish({
      destination: `/app/projects/${projectId}/chat.send`,
      body: JSON.stringify({ content: trimmed }),
    })
    return true
  }

  return { messages, connected, error, sendMessage }
}
