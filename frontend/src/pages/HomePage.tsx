import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { API_BASE_URL } from '../config'

gsap.registerPlugin(ScrollTrigger)

type HealthResponse = {
  status: string
}

function useBackendHealth() {
  return useQuery({
    queryKey: ['backend-health'],
    queryFn: async () => {
      const { data } = await axios.get<HealthResponse>(`${API_BASE_URL}/actuator/health`)
      return data
    },
    retry: false,
  })
}

const FEATURES = [
  {
    title: 'Kanban Board',
    description: 'Drag & Drop으로 Task 상태를 관리하고, 우선순위와 담당자를 한눈에 확인하세요.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="4" width="6" height="16" rx="1.5" />
        <rect x="9.5" y="4" width="6" height="10" rx="1.5" />
        <rect x="16" y="4" width="6" height="13" rx="1.5" />
      </svg>
    ),
  },
  {
    title: '실시간 알림',
    description: 'Task 배정, 댓글, 멘션까지 — 새로고침 없이 즉시 알림을 받습니다.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <path d="M6 8a6 6 0 0 1 12 0c0 4.5 1.5 6 1.5 6h-15S6 12.5 6 8Z" />
        <path d="M10 19a2 2 0 0 0 4 0" />
      </svg>
    ),
  },
  {
    title: '팀 채팅',
    description: '프로젝트별 채팅으로 빠르게 소통하고 맥락을 공유하세요.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <path d="M4 5.5A1.5 1.5 0 0 1 5.5 4h13A1.5 1.5 0 0 1 20 5.5v9A1.5 1.5 0 0 1 18.5 16H9l-4 4v-4H5.5A1.5 1.5 0 0 1 4 14.5v-9Z" />
      </svg>
    ),
  },
  {
    title: '파일 공유',
    description: '문서와 파일을 프로젝트에 첨부하고 팀원과 함께 관리하세요.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <path d="M4 6a2 2 0 0 1 2-2h4l2 2h6a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6Z" />
      </svg>
    ),
  },
]

export function HomePage() {
  const { data, isLoading, isError } = useBackendHealth()
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const ctx = gsap.context(() => {
      gsap
        .timeline({ defaults: { ease: 'power3.out' } })
        .from('[data-anim="nav"]', { opacity: 0, y: -16, duration: 0.4 })
        .from('[data-anim="hero-title"]', { opacity: 0, y: 20, duration: 0.5 }, '-=0.25')
        .from('[data-anim="hero-sub"]', { opacity: 0, y: 14, duration: 0.4 }, '-=0.3')
        .from('[data-anim="hero-cta"]', { opacity: 0, y: 14, duration: 0.4 }, '-=0.25')

      gsap.from('[data-anim="feature-card"]', {
        opacity: 0,
        y: 28,
        duration: 0.6,
        stagger: 0.12,
        ease: 'power2.out',
        scrollTrigger: {
          trigger: '[data-anim="features"]',
          start: 'top 80%',
        },
      })
    }, rootRef)

    return () => ctx.revert()
  }, [])

  return (
    <div ref={rootRef} className="min-h-screen bg-white text-slate-900">
      <nav data-anim="nav" className="mx-auto flex max-w-6xl items-center justify-between px-6 py-6">
        <span className="text-xl font-bold tracking-tight text-primary-600">TeamFlow</span>
        <div className="flex items-center gap-2">
          <Link to="/login" className="rounded-lg px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-900">
            로그인
          </Link>
          <Link
            to="/signup"
            className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-primary-700"
          >
            무료로 시작하기
          </Link>
        </div>
      </nav>

      <main>
        <section className="mx-auto max-w-6xl px-6 pb-24 pt-16 text-center sm:pt-24">
          <h1
            data-anim="hero-title"
            className="mx-auto max-w-3xl text-4xl font-extrabold leading-tight tracking-tight text-slate-900 sm:text-6xl"
          >
            팀의 작업을,
            <br />
            <span className="text-primary-600">하나의 흐름으로.</span>
          </h1>
          <p data-anim="hero-sub" className="mx-auto mt-6 max-w-xl text-lg text-slate-500">
            Task 관리, 실시간 알림과 채팅, 파일 공유까지 — 여러 도구를 오가지 않고 한 화면에서 프로젝트를 운영하세요.
          </p>
          <div data-anim="hero-cta" className="mt-10 flex items-center justify-center gap-4">
            <Link
              to="/signup"
              className="rounded-lg bg-primary-600 px-6 py-3 text-base font-semibold text-white shadow-sm transition hover:bg-primary-700"
            >
              무료로 시작하기
            </Link>
            <Link
              to="/login"
              className="rounded-lg border border-slate-200 px-6 py-3 text-base font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
            >
              로그인
            </Link>
          </div>
        </section>

        <section data-anim="features" className="border-t border-slate-100 bg-slate-50">
          <div className="mx-auto max-w-6xl px-6 py-20">
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
              {FEATURES.map((feature) => (
                <div
                  key={feature.title}
                  data-anim="feature-card"
                  className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"
                >
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-50 text-primary-600">
                    {feature.icon}
                  </div>
                  <h3 className="mt-4 text-base font-semibold text-slate-900">{feature.title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-slate-500">{feature.description}</p>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>

      <footer className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-3 px-6 py-8 text-sm text-slate-400 sm:flex-row">
        <span>&copy; 2026 TeamFlow</span>
        <span className="flex items-center gap-1.5">
          시스템 상태:
          {isLoading && <span className="text-slate-400">확인 중...</span>}
          {isError && <span className="text-red-500">● 연결 실패</span>}
          {data && <span className="text-emerald-500">● {data.status}</span>}
        </span>
      </footer>
    </div>
  )
}
