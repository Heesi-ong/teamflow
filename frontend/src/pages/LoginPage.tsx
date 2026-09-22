import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { authApi } from '../services/authApi'
import { useAuthStore } from '../store/authStore'

const inputClass =
  'w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100'

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
    <AuthLayout
      title="로그인"
      footer={
        <>
          계정이 없나요?{' '}
          <Link to="/signup" className="font-medium text-primary-600 hover:text-primary-700">
            회원가입
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-3.5">
        <input
          type="email"
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          className={inputClass}
        />
        <input
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          className={inputClass}
        />
        {error && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}
        <button
          type="submit"
          disabled={submitting}
          className="mt-1 rounded-lg bg-primary-600 px-3.5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700 disabled:opacity-50"
        >
          로그인
        </button>
      </form>
    </AuthLayout>
  )
}
