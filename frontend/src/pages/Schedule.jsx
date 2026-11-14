import { useEffect, useState } from 'react'
import { api, longDate, parseYmd, tabDate, ymd } from '../api'
import Status from '../components/Status'

const addDays = (d, n) => new Date(d.getFullYear(), d.getMonth(), d.getDate() + n)

export default function Schedule() {
  const [date, setDate] = useState(() => new Date())
  const [games, setGames] = useState(null)
  const [err, setErr] = useState('')
  const [calOpen, setCalOpen] = useState(false)
  const [calDays, setCalDays] = useState([])
  const [calErr, setCalErr] = useState('')

  const key = ymd(date)
  const tabs = [-3, -2, -1, 0, 1, 2, 3].map((n) => addDays(date, n))

  useEffect(() => {
    let alive = true
    api('/api/schedule?date=' + key)
      .then((res) => {
        if (!alive) return
        setGames(res.games)
        setErr('')
      })
      .catch((e) => {
        if (!alive) return
        setGames(null)
        setErr(e.message)
      })
    return () => {
      alive = false
    }
  }, [key])

  // 날짜 선택: 그 달에 경기 있는 날 목록
  useEffect(() => {
    if (!calOpen) return
    let alive = true
    api('/api/schedule/month?month=' + key.slice(0, 7))
      .then((res) => {
        if (!alive) return
        setCalDays(res.days)
        setCalErr('')
      })
      .catch((e) => {
        if (!alive) return
        setCalDays([])
        setCalErr(e.message)
      })
    return () => {
      alive = false
    }
  }, [calOpen, key])

  const pick = (s) => {
    setDate(parseYmd(s))
  }

  return (
    <div>
      <section className="card date-bar">
        <button className="btn sq" onClick={() => setDate(addDays(date, -1))}>
          ←
        </button>
        <h2>{longDate(date)}</h2>
        <button className="btn sq" onClick={() => setDate(addDays(date, 1))}>
          →
        </button>
        <span className="spacer" />
        <button className="btn" onClick={() => setCalOpen(!calOpen)}>
          날짜 선택 ▾
        </button>
      </section>

      {calOpen && (
        <div className="card">
          <input className="input" type="date" value={key} onChange={(e) => e.target.value && pick(e.target.value)} />
          <p className="muted tiny" style={{ marginTop: 12 }}>
            {key.slice(0, 7)} 경기가 있는 날
          </p>
          <Status error={calErr} />
          <div className="cal-days">
            {calDays.length === 0 && !calErr && <span className="muted small">경기가 있는 날이 없어요.</span>}
            {calDays.map((d) => (
              <button key={d.date} className={d.canceled ? 'canceled' : ''} onClick={() => pick(d.date)}>
                {Number(d.date.slice(8))}일 · {d.canceled ? '취소' : `${d.games}경기`}
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="day-tabs">
        {tabs.map((d, i) => (
          <button key={i} className={'day' + (i === 3 ? ' on' : '')} onClick={() => setDate(d)}>
            {tabDate(d)}
          </button>
        ))}
      </div>

      <div className="card-head plain">
        <h3>전체 경기</h3>
        <span className="muted tiny">응원팀 경기 우선</span>
      </div>

      <Status loading={!games && !err} error={err} />
      {games && games.length === 0 && (
        <article className="game notice empty-day">
          <h3>경기가 없는 날이에요</h3>
          <p className="muted small">다른 날짜를 골라보세요.</p>
        </article>
      )}
      {games && games.length > 0 && (
        <div className="games">
          {games.map((g) => (
            <article key={g.gameId} className={'game' + (g.myTeam ? ' mine' : '')}>
              <div className="game-top">
                <b className={g.myTeam ? 'hot' : 'muted'}>{g.statusText}</b>
                <span className="muted tiny">
                  {g.stageText ? `${g.stageText} · ` : ''}
                  {g.stadium} · {g.time}
                </span>
              </div>
              <div className="game-mid">
                <b className={g.myTeam ? 'hot' : ''}>{g.away.name}</b>
                <span className="game-score">
                  {g.status === 'FINISHED' ? `${g.away.score} : ${g.home.score}` : g.status === 'CANCELED' ? '—' : g.time}
                </span>
                <b>{g.home.name}</b>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  )
}
