import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fileApi } from '../services/fileApi'

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

export function FilesPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)

  const filesQuery = useQuery({ queryKey: ['files', id], queryFn: () => fileApi.list(id) })

  const uploadMutation = useMutation({
    mutationFn: (file: File) => fileApi.upload(id, file),
    onSuccess: () => {
      setError(null)
      queryClient.invalidateQueries({ queryKey: ['files', id] })
    },
    onError: (err: any) => setError(err.response?.data?.message ?? '업로드에 실패했습니다.'),
  })

  const removeMutation = useMutation({
    mutationFn: (fileId: number) => fileApi.remove(id, fileId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['files', id] }),
  })

  async function handleDownload(fileId: number) {
    const url = await fileApi.downloadUrl(id, fileId)
    window.open(url, '_blank')
  }

  return (
    <main className="min-h-screen bg-slate-50 p-6">
      <Link to={`/projects/${id}`} className="text-sm text-blue-600 underline">
        ← 프로젝트로
      </Link>
      <div className="mt-2 flex items-center justify-between">
        <h1 className="text-xl font-bold text-slate-800">파일</h1>
        <button onClick={() => fileInputRef.current?.click()} className="rounded bg-blue-600 px-3 py-2 text-sm text-white">
          업로드
        </button>
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          onChange={(e) => {
            const file = e.target.files?.[0]
            if (file) uploadMutation.mutate(file)
            e.target.value = ''
          }}
        />
      </div>
      {uploadMutation.isPending && <p className="mt-2 text-sm text-slate-500">업로드 중...</p>}
      {error && <p className="mt-2 text-sm text-red-500">{error}</p>}

      <table className="mt-4 w-full rounded border border-slate-200 bg-white text-sm">
        <thead>
          <tr className="border-b border-slate-100 text-left text-slate-500">
            <th className="p-2">파일명</th>
            <th className="p-2">크기</th>
            <th className="p-2">업로더</th>
            <th className="p-2">업로드일</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {filesQuery.data?.content.map((f) => (
            <tr key={f.id} className="border-b border-slate-50">
              <td className="p-2">
                <button onClick={() => handleDownload(f.id)} className="text-blue-600 hover:underline">
                  {f.fileName}
                </button>
              </td>
              <td className="p-2 text-slate-500">{formatSize(f.fileSize)}</td>
              <td className="p-2 text-slate-500">{f.uploaderName}</td>
              <td className="p-2 text-slate-500">{new Date(f.createdAt).toLocaleDateString()}</td>
              <td className="p-2 text-right">
                <button onClick={() => removeMutation.mutate(f.id)} className="text-xs text-red-400 hover:underline">
                  삭제
                </button>
              </td>
            </tr>
          ))}
          {filesQuery.data?.content.length === 0 && (
            <tr>
              <td colSpan={5} className="p-4 text-center text-slate-400">
                업로드된 파일이 없습니다.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </main>
  )
}
