import axios from 'axios'
import { http } from './http'
import type { PageResponse } from './projectApi'

export const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024

export interface ProjectFile {
  id: number
  projectId: number
  taskId: number | null
  uploaderId: number
  uploaderName: string
  fileName: string
  fileSize: number
  contentType: string
  createdAt: string
}

export const fileApi = {
  list: (projectId: number, taskId?: number) =>
    http.get<PageResponse<ProjectFile>>(`/projects/${projectId}/files`, { params: { taskId, size: 50 } }).then((res) => res.data),
  downloadUrl: (projectId: number, fileId: number) =>
    http.get<{ presignedUrl: string }>(`/projects/${projectId}/files/${fileId}/download-url`).then((res) => res.data.presignedUrl),
  remove: (projectId: number, fileId: number) => http.delete(`/projects/${projectId}/files/${fileId}`),

  // 11-file-storage-design.md 업로드 흐름: presigned URL 발급 -> S3에 직접 PUT -> 메타데이터 등록.
  async upload(projectId: number, file: File, taskId?: number): Promise<ProjectFile> {
    if (file.size > MAX_FILE_SIZE_BYTES) {
      throw new Error('파일은 최대 50MB까지 업로드할 수 있습니다.')
    }
    const { data: presigned } = await http.post<{ presignedUrl: string; s3Key: string }>(
      `/projects/${projectId}/files/presigned-url`,
      { fileName: file.name, contentType: file.type || 'application/octet-stream', fileSize: file.size },
    )
    // S3로의 PUT은 Authorization 헤더를 붙이면 안 되므로 baseURL/인터셉터가 있는 http 인스턴스를 쓰지 않는다.
    await axios.put(presigned.presignedUrl, file, { headers: { 'Content-Type': file.type || 'application/octet-stream' } })
    const { data: registered } = await http.post<ProjectFile>(`/projects/${projectId}/files`, {
      s3Key: presigned.s3Key,
      fileName: file.name,
      fileSize: file.size,
      contentType: file.type || 'application/octet-stream',
      taskId,
    })
    return registered
  },
}
