import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

type AuthLayoutProps = {
  title: string
  children: ReactNode
  footer: ReactNode
}

export function AuthLayout({ title, children, footer }: AuthLayoutProps) {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-slate-50 px-4 py-12">
      <Link to="/" className="mb-8 text-xl font-bold tracking-tight text-primary-600">
        TeamFlow
      </Link>
      <div className="w-full max-w-sm rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="text-xl font-semibold text-slate-900">{title}</h1>
        {children}
      </div>
      <p className="mt-6 text-sm text-slate-500">{footer}</p>
    </main>
  )
}
