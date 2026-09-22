import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { documentApi, type ProjectDocument } from '../services/documentApi'

function DocumentEditor({ projectId, document, onDeleted }: {
  projectId: number
  document: ProjectDocument
  onDeleted: () => void
}) {
  const queryClient = useQueryClient()
  const [title, setTitle] = useState(document.title)
  const [content, setContent] = useState(document.content ?? '')
  const saveMutation = useMutation({
    mutationFn: () => documentApi.update(projectId, document.id, { title, content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents', projectId] })
      queryClient.invalidateQueries({ queryKey: ['document', projectId, document.id] })
    },
  })
  const deleteMutation = useMutation({
    mutationFn: () => documentApi.remove(projectId, document.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents', projectId] })
      onDeleted()
    },
  })

  return (
    <>
      <input value={title} maxLength={255} onChange={(e) => setTitle(e.target.value)} className="w-full border-b border-slate-200 pb-2 text-lg font-semibold text-slate-900 focus:border-primary-400 focus:outline-none" />
      <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={16} className="mt-3 w-full resize-none text-sm text-slate-700 focus:outline-none" placeholder="내용을 입력하세요" />
      <div className="mt-3 flex gap-2">
        <button disabled={!title.trim()} onClick={() => saveMutation.mutate()} className="rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-50">저장</button>
        <button onClick={() => deleteMutation.mutate()} className="rounded-lg bg-red-50 px-3.5 py-2 text-sm font-medium text-red-500 hover:bg-red-100">삭제</button>
      </div>
    </>
  )
}

export function DocumentsPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const id = Number(projectId)
  const queryClient = useQueryClient()
  const [selectedId, setSelectedId] = useState<number | null>(null)

  const listQuery = useQuery({ queryKey: ['documents', id], queryFn: () => documentApi.list(id) })
  const docQuery = useQuery({
    queryKey: ['document', id, selectedId],
    queryFn: () => documentApi.get(id, selectedId!),
    enabled: selectedId != null,
  })

  const createMutation = useMutation({
    mutationFn: () => documentApi.create(id, { title: '새 문서', content: '' }),
    onSuccess: (doc) => {
      queryClient.invalidateQueries({ queryKey: ['documents', id] })
      setSelectedId(doc.id)
    },
  })

  return (
    <main className="min-h-screen bg-slate-50 p-6">
      <Link to={`/projects/${id}`} className="text-sm font-medium text-slate-500 hover:text-slate-700">
        ← 프로젝트로
      </Link>
      <h1 className="mt-2 text-xl font-bold text-slate-900">문서</h1>

      <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-[240px_1fr]">
        <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
          <button
            onClick={() => createMutation.mutate()}
            className="w-full border-b border-slate-100 p-2.5 text-sm font-medium text-primary-600 hover:bg-primary-50"
          >
            + 새 문서
          </button>
          <ul>
            {listQuery.data?.content.map((doc) => (
              <li key={doc.id}>
                <button
                  onClick={() => setSelectedId(doc.id)}
                  className={`block w-full truncate p-2.5 text-left text-sm hover:bg-slate-50 ${
                    selectedId === doc.id ? 'bg-primary-50 text-primary-700' : 'text-slate-700'
                  }`}
                >
                  {doc.title}
                </button>
              </li>
            ))}
            {listQuery.data?.content.length === 0 && <li className="p-2.5 text-sm text-slate-400">문서가 없습니다.</li>}
          </ul>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          {selectedId == null && <p className="text-sm text-slate-400">왼쪽에서 문서를 선택하거나 새로 만드세요.</p>}
          {selectedId != null && docQuery.data && (
            <DocumentEditor key={`${docQuery.data.id}-${docQuery.data.updatedAt}`} projectId={id} document={docQuery.data} onDeleted={() => setSelectedId(null)} />
          )}
        </div>
      </div>
    </main>
  )
}
