import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useProjectChat } from '../hooks/useProjectChat'

export function ChatPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const [input, setInput] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)
  const { messages, connected, error, sendMessage } = useProjectChat(id, true)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  function handleSend(e: FormEvent) {
    e.preventDefault()
    if (sendMessage(input)) setInput('')
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-2xl flex-col bg-slate-50 p-6">
      <Link to={`/projects/${id}`} className="text-sm font-medium text-slate-500 hover:text-slate-700">
        ← 프로젝트로
      </Link>
      <div className="mt-2 flex items-center gap-2">
        <h1 className="text-xl font-bold text-slate-900">채팅</h1>
        <span
          className={`h-2 w-2 rounded-full ${connected ? 'bg-emerald-500' : 'bg-slate-300'}`}
          title={connected ? '연결됨' : '연결 중'}
        />
      </div>
      {error && <p className="mt-1 text-sm text-red-500">{error}</p>}

      <div className="mt-3 flex-1 space-y-2 overflow-y-auto rounded-xl border border-slate-200 bg-white p-4">
        {messages.map((m) => (
          <div key={m.id} className="max-w-xs rounded-xl bg-slate-100 px-3.5 py-2.5">
            <p className="text-xs font-semibold text-primary-600">{m.authorName}</p>
            <p className="mt-0.5 text-sm text-slate-800">{m.content}</p>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      <form onSubmit={handleSend} className="mt-3 flex gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="메시지 입력"
          className="flex-1 rounded-lg border border-slate-300 px-3.5 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100"
        />
        <button
          type="submit"
          disabled={!connected}
          className="rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-50"
        >
          전송
        </button>
      </form>
    </main>
  )
}
