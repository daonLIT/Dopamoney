// 배포 환경변수에 스킴이 빠지거나 끝에 /가 붙어도 동작하게 정리한다.
const RAW = (import.meta.env.VITE_API_URL || '').trim().replace(/\/+$/, '')
const BASE = RAW && !/^https?:\/\//.test(RAW) ? `https://${RAW}` : RAW

export const store = {
  get(k) {
    try { return localStorage.getItem(k) } catch { return null }
  },
  set(k, v) {
    try {
      if (v == null) localStorage.removeItem(k)
      else localStorage.setItem(k, v)
    } catch { /* ignore */ }
  },
}

export const getToken = () => store.get('token')
export const getSavedUser = () => {
  try { return JSON.parse(store.get('user')) } catch { return null }
}
export const saveSession = (token, user) => {
  store.set('token', token)
  store.set('user', user ? JSON.stringify(user) : null)
}
export const clearSession = () => saveSession(null, null)

export async function api(path, { method = 'GET', body } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  let res
  try {
    res = await fetch(BASE + path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined })
  } catch {
    throw new Error('서버에 연결할 수 없어요')
  }
  let data = null
  if (res.status !== 204) {
    try { data = await res.json() } catch { data = null }
  }
  if (!res.ok) {
    if (res.status === 401 && token) {
      clearSession()
      window.dispatchEvent(new Event('unauthorized'))
    }
    throw new Error(data?.message || `오류가 발생했어요 (${res.status})`)
  }
  return data
}

export const won = (n) => Number(n || 0).toLocaleString('ko-KR')
export const pad = (n) => String(n).padStart(2, '0')
const DOW = '일월화수목금토'
export const ymd = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
export const parseYmd = (s) => { const [y, m, d] = s.split('-').map(Number); return new Date(y, m - 1, d) }
export const shortDate = (s) => { const d = parseYmd(s); return `${pad(d.getMonth() + 1)}.${pad(d.getDate())} (${DOW[d.getDay()]})` }
export const longDate = (d) => `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일 ${DOW[d.getDay()]}요일`
export const tabDate = (d) => `${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${DOW[d.getDay()]}`
