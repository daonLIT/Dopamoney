import { useEffect, useState } from 'react'
import { api } from '../api'
import Status from '../components/Status'

export default function TeamSelect({ onDone, initialNick = '', initialTeamCode = null }) {
  const [teams, setTeams] = useState([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')
  const [picked, setPicked] = useState(initialTeamCode)
  const [nick, setNick] = useState(initialNick)
  const [busy, setBusy] = useState(false)
  const nickOk = nick.trim().length >= 2 && nick.trim().length <= 10
  const ready = nickOk && picked
  const pickedTeam = teams.find((t) => t.code === picked)

  useEffect(() => {
    api('/api/teams')
      .then(setTeams)
      .catch((e) => setErr(e.message))
      .finally(() => setLoading(false))
  }, [])

  const submit = async () => {
    setErr('')
    setBusy(true)
    try {
      const me = await api('/api/users/me/profile', { method: 'PUT', body: { nickname: nick.trim(), teamCode: picked } })
      onDone(me)
    } catch (e) {
      setErr(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="team-page container">
      <div className="team-top">
        <strong className="team-logo">본전</strong>
        <span className="muted small">나의 응원팀 설정</span>
      </div>
      <p className="step">01 &nbsp;로그인 &nbsp;&nbsp; / &nbsp;&nbsp; 02 &nbsp;응원팀 선택</p>
      <h1 className="team-title">어느 팀을 응원하시나요?</h1>
      <p className="muted team-sub">선택한 팀을 기준으로 경기 일정과 본전 현황을 보여드려요.</p>

      <div className="nick-field">
        <label>닉네임 *</label>
        <input
          className="input"
          maxLength={10}
          placeholder="리더보드에 보여줄 닉네임 (2~10자)"
          value={nick}
          onChange={(e) => setNick(e.target.value)}
        />
      </div>

      <Status loading={loading} />
      <div className="team-grid">
        {teams.map((t) => (
          <button
            key={t.code}
            className={'team-card' + (picked === t.code ? ' selected' : '')}
            onClick={() => setPicked(t.code)}
          >
            <span className="team-badge" style={{ background: t.color }}>
              {t.code}
            </span>
            <span className="team-name">{t.name}</span>
          </button>
        ))}
      </div>

      <Status error={err} />
      <div className="team-bottom">
        <p className="muted small">팀은 나중에 내 정보에서 언제든 바꿀 수 있어요.</p>
        <button
          className={'btn lg team-submit' + (ready ? ' primary' : '')}
          disabled={!ready || busy}
          onClick={submit}
        >
          {ready
            ? `${pickedTeam?.name || picked} 응원 시작 →`
            : !nickOk
              ? '닉네임을 입력해주세요'
              : '응원팀을 선택해주세요'}
        </button>
      </div>
    </div>
  )
}
