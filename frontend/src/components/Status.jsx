// 로딩/오류 공통 표시 (텍스트)
export default function Status({ loading, error }) {
  if (loading) return <p className="muted">불러오는 중…</p>
  if (error) return <p className="err-msg">{error}</p>
  return null
}
