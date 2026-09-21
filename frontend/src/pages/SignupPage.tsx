import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../services/authApi'

export function SignupPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await authApi.signup({ email, password, name })
      navigate('/login')
    } catch (err: any) {
      setError(err.response?.data?.message ?? '회원가입에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-slate-50">
      <h1 className="text-2xl font-bold text-blue-600">회원가입</h1>
      <form onSubmit={handleSubmit} className="flex w-72 flex-col gap-3">
        <input
          type="text"
          placeholder="이름"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          className="rounded border border-slate-300 px-3 py-2"
        />
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
          placeholder="비밀번호 (영문+숫자 8자 이상)"
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
          가입하기
        </button>
      </form>
      <p className="text-sm text-slate-500">
        이미 계정이 있나요?{' '}
        <Link to="/login" className="text-blue-600 underline">
          로그인
        </Link>
      </p>
    </main>
  )
}
