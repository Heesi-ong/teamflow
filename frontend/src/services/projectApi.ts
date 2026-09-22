import { http } from './http'

export type ProjectStatus = 'PLANNING' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED'
export type ProjectRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'GUEST'
export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'REVOKED'

export interface ProjectSummary {
  id: number
  name: string
  description: string | null
  status: ProjectStatus
  startDate: string | null
  endDate: string | null
}

export interface Project extends ProjectSummary {
  ownerId: number
  createdAt: string
  updatedAt: string
}

export interface ProjectUpdateBody {
  name?: string
  description?: string
  status?: ProjectStatus
  startDate?: string | null
  endDate?: string | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
}

export interface ProjectMember {
  id: number
  userId: number
  userName: string
  userEmail: string
  role: ProjectRole
  joinedAt: string
}

export interface Invitation {
  id: number
  email: string | null
  role: ProjectRole
  status: InvitationStatus
  expiresAt: string
  createdAt: string
}

export interface InvitationCreated {
  invitationId: number
  token: string
  role: ProjectRole
  expiresAt: string
}

export const projectApi = {
  list: () => http.get<PageResponse<ProjectSummary>>('/projects').then((res) => res.data),
  create: (body: { name: string; description: string; startDate: string | null; endDate: string | null }) =>
    http.post<Project>('/projects', body).then((res) => res.data),
  get: (projectId: number) => http.get<Project>(`/projects/${projectId}`).then((res) => res.data),
  update: (projectId: number, body: ProjectUpdateBody) =>
    http.patch<Project>(`/projects/${projectId}`, body).then((res) => res.data),
  remove: (projectId: number) => http.delete(`/projects/${projectId}`),
  members: (projectId: number) => http.get<ProjectMember[]>(`/projects/${projectId}/members`).then((res) => res.data),
  invite: (projectId: number, body: { email?: string; role?: ProjectRole }) =>
    http.post<InvitationCreated>(`/projects/${projectId}/invitations`, body).then((res) => res.data),
  invitations: (projectId: number) => http.get<Invitation[]>(`/projects/${projectId}/invitations`).then((res) => res.data),
  revokeInvitation: (projectId: number, invitationId: number) =>
    http.delete(`/projects/${projectId}/invitations/${invitationId}`),
  acceptInvitation: (token: string) => http.post<ProjectMember>(`/invitations/${token}/accept`).then((res) => res.data),
  removeMember: (projectId: number, memberId: number) => http.delete(`/projects/${projectId}/members/${memberId}`),
  changeRole: (projectId: number, memberId: number, role: Exclude<ProjectRole, 'OWNER'>) =>
    http.patch<ProjectMember>(`/projects/${projectId}/members/${memberId}/role`, { role }).then((res) => res.data),
  transferOwnership: (projectId: number, memberId: number) =>
    http.patch<ProjectMember[]>(`/projects/${projectId}/members/${memberId}/transfer-ownership`).then((res) => res.data),
  leave: (projectId: number) => http.delete(`/projects/${projectId}/members/me`),
}
