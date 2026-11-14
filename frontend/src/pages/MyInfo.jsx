import { useEffect, useState } from 'react'
import baseball from '../assets/baseball.svg'
import sparkles from '../assets/sparkles.svg'
import { api, pad } from '../api'
import Status from '../components/Status'

export default function MyInfo({ onChangeUser, onLogout }) {
  const [me, setMe] = useState(null)
  const [teams, setTeams] = useState([])
  const [err, setErr] = useState('')
  const [msg, setMsg] = useState('')
  const [edit, setEdit] = useState(null) // 'nickname' | 'team' | 'password'
  const [nick, setNick] = useState('')
  const [teamCode, setTeamCode] = useState('')
  const [curPw, setCurPw] = useState('')
  const [newPw, setNewPw] = useState('')

  useEffect(() => {
    api('/api/users/me')
      .then(setMe)
      .catch((e) => setErr(e.message))
    api('/api/teams')
      .then(setTeams)
      .catch(() => {})
  }, [])

  if (!me) return <Status loading={!err} error={err} />

  const open = (k) => {
    setErr('')
    setMsg('')
    setNick(me.nickname)
    setTeamCode(me.team?.code || '')
    setCurPw('')
    setNewPw('')
    setEdit(edit === k ? null : k)
  }

  const saveProfile = async () => {
    setErr('')
    try {
      const res = await api('/api/users/me/profile', {
        method: 'PUT',
        body: { nickname: nick.trim(), teamCode: teamCode || me.team?.code },
      })
      setMe(res)
      onChangeUser(res)
      setEdit(null)
      setMsg('변경했어요!')
    } catch (e) {
      setErr(e.message)
    }
  }

  const savePassword = async () => {
    setErr('')
    try {
      await api('/api/users/me/password', { method: 'PATCH', body: { currentPassword: curPw, newPassword: newPw } })
      setEdit(null)
      setMsg('비밀번호를 변경했어요!')
    } catch (e) {
      setErr(e.message)
    }
  }

  const rows = [
    ['nickname', '이름', me.nickname],
    ['team', '응원팀', me.team?.name || '-'],
    ['password', '비밀번호', '••••••••••••'],
  ]

  return (
    <div className="grid-my">
      <section className="card dark identity">
        <div className="ball avatar">
          <img src={baseball} alt="" className="ball-img" />
          <img src={sparkles} alt="" className="ball-sparkle" />
        </div>
        <h2 className="white">{me.nickname}</h2>
        <p className="soft-light small">@{me.loginId}</p>
        <p className="team-accent">{me.team?.name}</p>
        <hr />
        <div className="two-col">
          <div>
            <p className="stat white">{me.visitCount}회</p>
            <p className="soft-light tiny">직관 기록</p>
          </div>
          <div>
            <p className="stat white">LV. {pad(me.level.lv)}</p>
            <p className="soft-light tiny">{me.level.title}</p>
          </div>
        </div>
      </section>

      <section className="card settings">
        <h3 className="lg">계정 설정</h3>
        {rows.map(([k, label, value]) => (
          <div key={k}>
            <div className="set-row">
              <div>
                <p className="muted tiny">{label}</p>
                <p className="set-val">{value}</p>
              </div>
              <button className="btn" onClick={() => open(k)}>
                {edit === k ? '닫기' : '변경'}
              </button>
            </div>
            {edit === k && k === 'nickname' && (
              <div className="inline-form">
                <input className="input" maxLength={10} value={nick} onChange={(e) => setNick(e.target.value)} />
                <button className="btn primary" onClick={saveProfile}>
                  저장
                </button>
              </div>
            )}
            {edit === k && k === 'team' && (
              <div className="inline-form">
                <select className="input" value={teamCode} onChange={(e) => setTeamCode(e.target.value)}>
                  {teams.map((t) => (
                    <option key={t.code} value={t.code}>
                      {t.name}
                    </option>
                  ))}
                </select>
                <button className="btn primary" onClick={saveProfile}>
                  저장
                </button>
              </div>
            )}
            {edit === k && k === 'password' && (
              <div className="inline-form">
                <input
                  className="input"
                  type="password"
                  placeholder="현재 비밀번호"
                  value={curPw}
                  onChange={(e) => setCurPw(e.target.value)}
                />
                <input
                  className="input"
                  type="password"
                  placeholder="새 비밀번호 (8자 이상)"
                  value={newPw}
                  onChange={(e) => setNewPw(e.target.value)}
                />
                <button className="btn primary" onClick={savePassword}>
                  저장
                </button>
              </div>
            )}
          </div>
        ))}
        <Status error={err} />
        {msg && <p className="ok-msg">{msg}</p>}
        <p className="muted tiny">팬심은 그대로, 정보는 언제든 새롭게.</p>
      </section>

      <section className="card sand collection">
        <h2>
          응원팀이 못할수록
          <br />
          내 야구공은 더 화려하게.
        </h2>
        <p className="muted small">
          손해도 나만의 컬렉션이 되는 곳.
          <br />
          다음 레벨의 야구공을 기대해보세요.
        </p>
      </section>

      <section className="card actions">
        <div>
          <h3>잠깐, 쉬어가도 괜찮아요.</h3>
          <p className="muted small">다음 경기 날, 여기서 다시 만나요.</p>
          <p className="muted tiny">회원 탈퇴 기능은 추후 제공될 예정이에요.</p>
        </div>
        <button className="btn" onClick={onLogout}>
          로그아웃
        </button>
      </section>
    </div>
  )
}
