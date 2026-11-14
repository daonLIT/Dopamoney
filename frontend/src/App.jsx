import { useEffect, useState } from 'react'
import './App.css'
import Layout from './components/Layout'
import Login from './pages/Login'
import TeamSelect from './pages/TeamSelect'
import Home from './pages/Home'
import Record from './pages/Record'
import Schedule from './pages/Schedule'
import Leaderboard from './pages/Leaderboard'
import MyInfo from './pages/MyInfo'
import { api, clearSession, getSavedUser, getToken, saveSession } from './api'

// 라우터 라이브러리 없이 상태로 화면 전환 (해커톤용)
const HEADINGS = {
  home: ['MY FAN REPORT', '오늘도 본전 생각, 하셨나요?'],
  record: ['FAN DIARY', '경기는 끝나도, 기록은 남으니까.'],
  schedule: ['GAME DAY', '오늘의 야구, 미리 보기.'],
  leaderboard: ['HALL OF PAIN', '누가 가장 억울할까?'],
  myinfo: ['MY CLUB', '내 팬심을 소개합니다.'],
}

const needsProfile = (u) => !u || !u.nickname || !u.teamCode

function App() {
  const [user, setUser] = useState(() => (getToken() ? getSavedUser() : null))
  const [page, setPage] = useState(() => {
    const u = getToken() ? getSavedUser() : null
    return !u ? 'login' : needsProfile(u) ? 'team' : 'home'
  })

  // 401 이면 로그인 화면으로
  useEffect(() => {
    const h = () => {
      setUser(null)
      setPage('login')
    }
    window.addEventListener('unauthorized', h)
    return () => window.removeEventListener('unauthorized', h)
  }, [])

  const updateUser = (u) => {
    const merged = { ...user, ...u }
    setUser(merged)
    saveSession(getToken(), merged)
  }

  const logout = async () => {
    try {
      await api('/api/auth/logout', { method: 'POST' })
    } catch {
      /* 실패해도 로컬 로그아웃 */
    }
    clearSession()
    setUser(null)
    setPage('login')
  }

  if (page === 'login') {
    return (
      <Login
        onLogin={(u) => {
          setUser(u)
          setPage(needsProfile(u) ? 'team' : 'home')
        }}
      />
    )
  }
  if (page === 'team') {
    return (
      <TeamSelect
        initialNick={user?.nickname || ''}
        initialTeamCode={user?.teamCode || null}
        onDone={(me) => {
          updateUser({ nickname: me.nickname, teamCode: me.team?.code })
          setPage('home')
        }}
      />
    )
  }

  const [eyebrow, title] = HEADINGS[page]
  return (
    <Layout page={page} go={setPage} user={user} eyebrow={eyebrow} title={title}>
      {page === 'home' && <Home go={setPage} />}
      {page === 'record' && <Record myTeamCode={user.teamCode} />}
      {page === 'schedule' && <Schedule />}
      {page === 'leaderboard' && <Leaderboard />}
      {page === 'myinfo' && (
        <MyInfo
          onChangeUser={(me) => updateUser({ nickname: me.nickname, teamCode: me.team?.code })}
          onLogout={logout}
        />
      )}
    </Layout>
  )
}

export default App
