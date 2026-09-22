import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { lazy, Suspense, useEffect, useState, type ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { NotificationBell } from '../components/NotificationBell'
import { refreshAccessToken } from '../services/http'
import { useAuthStore } from '../store/authStore'

const queryClient = new QueryClient()

const HomePage = lazy(() => import('../pages/HomePage').then((m) => ({ default: m.HomePage })))
const LoginPage = lazy(() => import('../pages/LoginPage').then((m) => ({ default: m.LoginPage })))
const SignupPage = lazy(() => import('../pages/SignupPage').then((m) => ({ default: m.SignupPage })))
const DashboardPage = lazy(() => import('../pages/DashboardPage').then((m) => ({ default: m.DashboardPage })))
const ProjectListPage = lazy(() => import('../pages/ProjectListPage').then((m) => ({ default: m.ProjectListPage })))
const ProjectCreatePage = lazy(() => import('../pages/ProjectCreatePage').then((m) => ({ default: m.ProjectCreatePage })))
const ProjectDetailPage = lazy(() => import('../pages/ProjectDetailPage').then((m) => ({ default: m.ProjectDetailPage })))
const ProjectMembersPage = lazy(() => import('../pages/ProjectMembersPage').then((m) => ({ default: m.ProjectMembersPage })))
const InvitationAcceptPage = lazy(() => import('../pages/InvitationAcceptPage').then((m) => ({ default: m.InvitationAcceptPage })))
const KanbanBoardPage = lazy(() => import('../pages/KanbanBoardPage').then((m) => ({ default: m.KanbanBoardPage })))
const ChatPage = lazy(() => import('../pages/ChatPage').then((m) => ({ default: m.ChatPage })))
const DocumentsPage = lazy(() => import('../pages/DocumentsPage').then((m) => ({ default: m.DocumentsPage })))
const FilesPage = lazy(() => import('../pages/FilesPage').then((m) => ({ default: m.FilesPage })))
const ProjectDashboardPage = lazy(() => import('../pages/ProjectDashboardPage').then((m) => ({ default: m.ProjectDashboardPage })))
const CalendarPage = lazy(() => import('../pages/CalendarPage').then((m) => ({ default: m.CalendarPage })))

function RequireAuth({ children }: { children: ReactNode }) {
  const accessToken = useAuthStore((s) => s.accessToken)
  const location = useLocation()
  if (!accessToken) {
    return <Navigate to="/login" replace state={{ returnTo: `${location.pathname}${location.search}` }} />
  }
  return children
}

// 하드 네비게이션(새 탭으로 초대 링크 열기 등) 직후에는 accessToken이 메모리에 없으므로,
// HttpOnly Refresh Token Cookie로 최초 1회 조용히 재발급을 시도해 세션을 복원한다.
function useSessionBootstrap() {
  const setAccessToken = useAuthStore((s) => s.setAccessToken)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    refreshAccessToken()
      .then((token) => token && setAccessToken(token))
      .finally(() => setReady(true))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return ready
}

export function App() {
  const sessionReady = useSessionBootstrap()

  if (!sessionReady) {
    return null
  }

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <NotificationBell />
        <Suspense fallback={<div className="min-h-screen bg-slate-50 p-6 text-sm text-slate-500">불러오는 중...</div>}>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/invitations/:token" element={<InvitationAcceptPage />} />
            <Route path="/dashboard" element={<RequireAuth><DashboardPage /></RequireAuth>} />
            <Route path="/projects" element={<RequireAuth><ProjectListPage /></RequireAuth>} />
            <Route path="/projects/new" element={<RequireAuth><ProjectCreatePage /></RequireAuth>} />
            <Route path="/projects/:projectId" element={<RequireAuth><ProjectDetailPage /></RequireAuth>} />
            <Route path="/projects/:projectId/board" element={<RequireAuth><KanbanBoardPage /></RequireAuth>} />
            <Route path="/projects/:projectId/chat" element={<RequireAuth><ChatPage /></RequireAuth>} />
            <Route path="/projects/:projectId/documents" element={<RequireAuth><DocumentsPage /></RequireAuth>} />
            <Route path="/projects/:projectId/files" element={<RequireAuth><FilesPage /></RequireAuth>} />
            <Route path="/projects/:projectId/dashboard" element={<RequireAuth><ProjectDashboardPage /></RequireAuth>} />
            <Route path="/projects/:projectId/calendar" element={<RequireAuth><CalendarPage /></RequireAuth>} />
            <Route path="/projects/:projectId/members" element={<RequireAuth><ProjectMembersPage /></RequireAuth>} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
