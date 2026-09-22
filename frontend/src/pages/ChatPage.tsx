import { Client } from '@stomp/stompjs'
import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { WS_BASE_URL } from '../config'
import { chatApi, type ChatMessage } from '../services/chatApi'
import { useAuthStore } from '../store/authStore'

export function ChatPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const accessToken = useAuthStore((s) => s.accessToken)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [connected, setConnected] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const clientRef = useRef<Client | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    chatApi.history(id).then((history) => setMessages([...history].reverse()))
  }, [id])

  // 10-realtime-architecture.md §2.1: 인증은 STOMP CONNECT 프레임의 Authorization 헤더로 전달한다.
  useEffect(() => {
    if (!accessToken) return
    const client = new Client({
      brokerURL: `${WS_BASE_URL}/ws/chat`,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 2000,
      onConnect: () => {
        setConnected(true)
        setError(null)
        client.subscribe(`/topic/projects/${id}/chat`, (frame) => {
          const message: ChatMessage = JSON.parse(frame.body)
          setMessages((prev) => [...prev, message])
        })
        client.subscribe('/user/queue/errors', (frame) => {
          const err = JSON.parse(frame.body)
          setError(err.message ?? '메시지 전송에 실패했습니다.')
        })
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setError('채팅 연결에 실패했습니다.'),
    })
    client.activate()
    clientRef.current = client
    return () => {
      client.deactivate()
    }
  }, [id, accessToken])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  function handleSend(e: React.FormEvent) {
    e.preventDefault()
    if (!input.trim() || !clientRef.current?.connected) return
    clientRef.current.publish({
      destination: `/app/projects/${id}/chat.send`,
      body: JSON.stringify({ content: input.trim() }),
    })
    setInput('')
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-2xl flex-col bg-slate-50 p-6">
      <Link to={`/projects/${id}`} className="text-sm text-blue-600 underline">
        ← 프로젝트로
      </Link>
      <div className="mt-2 flex items-center gap-2">
        <h1 className="text-xl font-bold text-slate-800">채팅</h1>
        <span className={`h-2 w-2 rounded-full ${connected ? 'bg-green-500' : 'bg-slate-300'}`} title={connected ? '연결됨' : '연결 중'} />
      </div>
      {error && <p className="mt-1 text-sm text-red-500">{error}</p>}

      <div className="mt-3 flex-1 space-y-2 overflow-y-auto rounded border border-slate-200 bg-white p-3">
        {messages.map((m) => (
          <div key={m.id} className="max-w-xs rounded-lg bg-slate-100 px-3 py-2">
            <p className="text-xs font-semibold text-slate-500">{m.authorName}</p>
            <p className="text-sm text-slate-800">{m.content}</p>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      <form onSubmit={handleSend} className="mt-3 flex gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="메시지 입력"
          className="flex-1 rounded border border-slate-300 px-3 py-2"
        />
        <button type="submit" disabled={!connected} className="rounded bg-blue-600 px-3 py-2 text-white disabled:opacity-50">
          전송
        </button>
      </form>
    </main>
  )
}
