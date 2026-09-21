import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../services/authApi'
import { useAuthStore } from '../store/authStore'

export function LoginPage() {
  const navigate = useNavigate()
  const setAccessToken = useAuthStore((s) => s.setAccessToken)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const { data } = await authApi.login({ email, password })
      setAccessToken(data.accessToken)
      navigate('/dashboard')
    } catch (err: any) {
      setError(err.response?.data?.message ?? '로그인에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-slate-50">
      <h1 className="text-2xl font-bold text-blue-600">로그인</h1>
      <form onSubmit={handleSubmit} className="flex w-72 flex-col gap-3">
        <input
          type="email"
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          className="rounded border border-slate-300 px-3 py-2"
        />
        <input
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          className="rounded border border-slate-300 px-3 py-2"
        />
        {error && <p className="text-sm text-red-500">{error}</p>}
        <button
          type="submit"
          disabled={submitting}
          className="rounded bg-blue-600 px-3 py-2 text-white disabled:opacity-50"
        >
          로그인
        </button>
      </form>
      <p className="text-sm text-slate-500">
        계정이 없나요?{' '}
        <Link to="/signup" className="text-blue-600 underline">
          회원가입
        </Link>
      </p>
    </main>
  )
}
