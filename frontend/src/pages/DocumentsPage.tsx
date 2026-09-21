import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { documentApi } from '../services/documentApi'

export function DocumentsPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const queryClient = useQueryClient()
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')

  const listQuery = useQuery({ queryKey: ['documents', id], queryFn: () => documentApi.list(id) })
  const docQuery = useQuery({
    queryKey: ['document', id, selectedId],
    queryFn: () => documentApi.get(id, selectedId!),
    enabled: selectedId != null,
  })

  useEffect(() => {
    if (docQuery.data) {
      setTitle(docQuery.data.title)
      setContent(docQuery.data.content ?? '')
    }
  }, [docQuery.data])

  const createMutation = useMutation({
    mutationFn: () => documentApi.create(id, { title: '새 문서', content: '' }),
    onSuccess: (doc) => {
      queryClient.invalidateQueries({ queryKey: ['documents', id] })
      setSelectedId(doc.id)
    },
  })

  const saveMutation = useMutation({
    mutationFn: () => documentApi.update(id, selectedId!, { title, content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents', id] })
      queryClient.invalidateQueries({ queryKey: ['document', id, selectedId] })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => documentApi.remove(id, selectedId!),
    onSuccess: () => {
      setSelectedId(null)
      queryClient.invalidateQueries({ queryKey: ['documents', id] })
    },
  })

  return (
    <main className="min-h-screen bg-slate-50 p-6">
      <Link to={`/projects/${id}`} className="text-sm text-blue-600 underline">
        ← 프로젝트로
      </Link>
      <h1 className="mt-2 text-xl font-bold text-slate-800">문서</h1>

      <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-[240px_1fr]">
        <div className="rounded border border-slate-200 bg-white">
          <button onClick={() => createMutation.mutate()} className="w-full border-b border-slate-100 p-2 text-sm text-blue-600 hover:bg-slate-50">
            + 새 문서
          </button>
          <ul>
            {listQuery.data?.content.map((doc) => (
              <li key={doc.id}>
                <button
                  onClick={() => setSelectedId(doc.id)}
                  className={`block w-full truncate p-2 text-left text-sm hover:bg-slate-50 ${selectedId === doc.id ? 'bg-blue-50 text-blue-700' : 'text-slate-700'}`}
                >
                  {doc.title}
                </button>
              </li>
            ))}
            {listQuery.data?.content.length === 0 && <li className="p-2 text-sm text-slate-400">문서가 없습니다.</li>}
          </ul>
        </div>

        <div className="rounded border border-slate-200 bg-white p-4">
          {selectedId == null && <p className="text-slate-400">왼쪽에서 문서를 선택하거나 새로 만드세요.</p>}
          {selectedId != null && docQuery.data && (
            <>
              <input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full border-b border-slate-200 pb-2 text-lg font-semibold focus:outline-none"
              />
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={16}
                className="mt-3 w-full resize-none text-sm text-slate-700 focus:outline-none"
                placeholder="내용을 입력하세요"
              />
              <div className="mt-3 flex gap-2">
                <button onClick={() => saveMutation.mutate()} className="rounded bg-blue-600 px-3 py-2 text-sm text-white">
                  저장
                </button>
                <button onClick={() => deleteMutation.mutate()} className="rounded bg-red-50 px-3 py-2 text-sm text-red-500">
                  삭제
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </main>
  )
}
