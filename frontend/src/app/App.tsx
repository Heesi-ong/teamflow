import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { SignupPage } from '../pages/SignupPage'
import { DashboardPage } from '../pages/DashboardPage'
import { ProjectListPage } from '../pages/ProjectListPage'
import { ProjectCreatePage } from '../pages/ProjectCreatePage'
import { ProjectDetailPage } from '../pages/ProjectDetailPage'
import { ProjectMembersPage } from '../pages/ProjectMembersPage'
import { InvitationAcceptPage } from '../pages/InvitationAcceptPage'
import { KanbanBoardPage } from '../pages/KanbanBoardPage'
import { ChatPage } from '../pages/ChatPage'
import { DocumentsPage } from '../pages/DocumentsPage'
import { FilesPage } from '../pages/FilesPage'
import { NotificationBell } from '../components/NotificationBell'
import { refreshAccessToken } from '../services/http'
import { useAuthStore } from '../store/authStore'

const queryClient = new QueryClient()

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
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/projects" element={<ProjectListPage />} />
          <Route path="/projects/new" element={<ProjectCreatePage />} />
          <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
          <Route path="/projects/:projectId/board" element={<KanbanBoardPage />} />
          <Route path="/projects/:projectId/chat" element={<ChatPage />} />
          <Route path="/projects/:projectId/documents" element={<DocumentsPage />} />
          <Route path="/projects/:projectId/files" element={<FilesPage />} />
          <Route path="/projects/:projectId/members" element={<ProjectMembersPage />} />
          <Route path="/invitations/:token" element={<InvitationAcceptPage />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
