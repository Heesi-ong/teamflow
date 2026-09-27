import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useProjectChat } from '../hooks/useProjectChat'

interface FloatingProjectChatProps {
  projectId: number
}

export function FloatingProjectChat({ projectId }: FloatingProjectChatProps) {
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)
  const { messages, connected, error, sendMessage } = useProjectChat(projectId, open)

  useEffect(() => {
    if (open) bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, open])

  function handleSend(event: FormEvent) {
    event.preventDefault()
    if (sendMessage(input)) setInput('')
  }

  if (!open) {
    return (
      <button
        type="button"
        aria-label="프로젝트 채팅 열기"
        aria-expanded="false"
        onClick={() => setOpen(true)}
        className="fixed bottom-5 right-5 z-40 flex items-center gap-2 rounded-full bg-primary-600 px-4 py-3 text-sm font-semibold text-white shadow-[0_12px_30px_rgba(79,70,229,0.35)] transition hover:-translate-y-0.5 hover:bg-primary-700 focus:outline-none focus:ring-4 focus:ring-primary-200"
      >
        <span aria-hidden="true" className="text-base">💬</span>
        채팅
      </button>
    )
  }

  return (
    <section
      aria-label="프로젝트 플로팅 채팅"
      className="fixed bottom-5 right-5 z-40 flex h-[min(560px,calc(100vh-2rem))] w-[min(380px,calc(100vw-2rem))] flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_18px_50px_rgba(15,23,42,0.2)]"
    >
      <header className="flex items-center justify-between bg-primary-600 px-4 py-3 text-white">
        <div>
          <h2 className="text-sm font-semibold">프로젝트 채팅</h2>
          <p className="mt-0.5 flex items-center gap-1.5 text-[11px] text-primary-100">
            <span className={`h-1.5 w-1.5 rounded-full ${connected ? 'bg-emerald-300' : 'bg-slate-300'}`} />
            {connected ? '실시간 연결됨' : '연결 중'}
          </p>
        </div>
        <button
          type="button"
          aria-label="프로젝트 채팅 최소화"
          onClick={() => setOpen(false)}
          className="rounded-lg px-2 py-1 text-lg leading-none text-primary-100 transition hover:bg-primary-500 hover:text-white"
        >
          −
        </button>
      </header>

      <div className="flex-1 space-y-2 overflow-y-auto bg-slate-50 p-3">
        {messages.length === 0 && <p className="py-8 text-center text-xs text-slate-400">아직 채팅 내역이 없습니다.</p>}
        {messages.map((message) => (
          <article key={message.id} className="max-w-[85%] rounded-xl border border-slate-200 bg-white px-3 py-2 shadow-sm">
            <p className="text-[11px] font-semibold text-primary-600">{message.authorName}</p>
            <p className="mt-0.5 break-words text-sm text-slate-800">{message.content}</p>
          </article>
        ))}
        <div ref={bottomRef} />
      </div>

      {error && <p className="border-t border-red-100 bg-red-50 px-3 py-2 text-xs text-red-600">{error}</p>}
      <form onSubmit={handleSend} className="flex gap-2 border-t border-slate-200 bg-white p-3">
        <input
          value={input}
          onChange={(event) => setInput(event.target.value)}
          placeholder="메시지 입력"
          aria-label="채팅 메시지"
          className="min-w-0 flex-1 rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-primary-500 focus:ring-2 focus:ring-primary-100"
        />
        <button
          type="submit"
          disabled={!connected || !input.trim()}
          className="rounded-xl bg-primary-600 px-3 py-2 text-sm font-semibold text-white transition hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          전송
        </button>
      </form>
    </section>
  )
}
