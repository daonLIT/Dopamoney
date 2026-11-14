import { useCallback, useEffect, useState } from 'react'
import { api, shortDate, won, ymd } from '../api'
import Status from '../components/Status'

const TAGS = [
  ['WIN', '승리'],
  ['LOSE', '패배'],
  ['REVERSE_LOSS', '역전패'],
  ['BIG_WIN', '대승'],
  ['VERIFIED', '직관 인증'],
]
const EMPTY_COST = { ticket: '', move: '', food: '' }

export default function Record() {
  const [date, setDate] = useState(ymd(new Date()))
  const [games, setGames] = useState([])
  const [gamesLoading, setGamesLoading] = useState(true)
  const [gameErr, setGameErr] = useState('')
  const [gameId, setGameId] = useState('')
  const [cost, setCost] = useState(EMPTY_COST)
  const [note, setNote] = useState('')
  const [tag, setTag] = useState(null)
  const [editId, setEditId] = useState(null)
  const [records, setRecords] = useState([])
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')
  const [formErr, setFormErr] = useState('')
  const [msg, setMsg] = useState('')

  const setC = (k) => (e) => setCost({ ...cost, [k]: e.target.value.replace(/\D/g, '') })

  const reload = useCallback(async () => {
    try {
      const [s, r] = await Promise.all([api('/api/records/summary'), api('/api/records?limit=10')])
      setSummary(s)
      setRecords(r)
      setErr('')
    } catch (e) {
      setErr(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    Promise.resolve().then(reload)
  }, [reload])

  // 선택한 날짜에 실제로 열린 경기 목록 (이 중에서만 고를 수 있다)
  useEffect(() => {
    if (!date) return
    let alive = true
    Promise.resolve()
      .then(() => {
        setGamesLoading(true)
        return api('/api/schedule?date=' + date)
      })
      .then((res) => {
        if (!alive) return
        const list = res.games || []
        setGames(list)
        setGameErr('')
        // 이미 고른 경기가 있으면 유지, 없으면 내 팀 경기를 기본 선택
        setGameId((prev) => {
          if (list.some((g) => g.gameId === prev && g.status !== 'CANCELED')) return prev
          return list.find((g) => g.myTeam && g.status !== 'CANCELED')?.gameId || ''
        })
      })
      .catch((e) => {
        if (!alive) return
        setGames([])
        setGameId('')
        setGameErr(e.message)
      })
      .finally(() => {
        if (alive) setGamesLoading(false)
      })
    return () => {
      alive = false
    }
  }, [date])

  const resetForm = () => {
    setNote('')
    setCost(EMPTY_COST)
    setTag(null)
    setEditId(null)
  }

  const submit = async () => {
    setFormErr('')
    setMsg('')
    if (!date || !note.trim()) {
      setFormErr('경기 날짜와 감상은 필수예요.')
      return
    }
    if (!gameId) {
      setFormErr('그 날 열린 경기 중에서 선택해주세요.')
      return
    }
    const body = {
      gameDate: date,
      gameId,
      ticketCost: Number(cost.ticket || 0),
      transportCost: Number(cost.move || 0),
      foodCost: Number(cost.food || 0),
      memo: note.trim(),
      tag,
    }
    try {
      await api(editId ? `/api/records/${editId}` : '/api/records', { method: editId ? 'PUT' : 'POST', body })
      setMsg(editId ? '기록을 수정했어요!' : '기록이 추가됐어요!')
      resetForm()
      reload()
    } catch (e) {
      setFormErr(e.message)
    }
  }

  const edit = (r) => {
    setEditId(r.id)
    setDate(r.gameDate)
    setGameId(r.game?.gameId ?? '')
    setCost({
      ticket: String(r.ticketCost || ''),
      move: String(r.transportCost || ''),
      food: String(r.foodCost || ''),
    })
    setNote(r.memo)
    setTag(r.tag)
    setMsg('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const remove = async (id) => {
    if (!confirm('이 기록을 삭제할까요?')) return
    try {
      await api(`/api/records/${id}`, { method: 'DELETE' })
      if (editId === id) resetForm()
      reload()
    } catch (e) {
      setErr(e.message)
    }
  }

  const resultText = (r) => (r.result === 'WIN' ? '승리' : r.result === 'LOSE' ? '패배' : '-')
  const matchText = (r) =>
    r.myScore != null && r.opponentScore != null
      ? `${r.myScore} : ${r.opponentScore}  ${r.opponent?.name ?? '-'}`
      : `vs ${r.opponent?.name ?? '-'}`
  const gameText = (r) => {
    if (!r.game) return matchText(r)
    const t = (x) => x.name.split(' ')[0]
    const { away, home } = r.game
    return away.score != null && home.score != null
      ? `${t(away)} ${away.score} : ${home.score} ${t(home)}`
      : `${t(away)} vs ${t(home)}`
  }

  return (
    <div className="grid-record">
      <section className="card form-card">
        <h3 className="lg">{editId ? '직관 기록 수정' : '오늘의 직관 남기기'}</h3>
        <p className="muted small">그 날 실제로 열린 경기 중에서 골라요. 지출은 선택이에요.</p>

        <div className="two-col gap">
          <div>
            <label>경기 날짜 *</label>
            <input className="input" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </div>
          <div>
            <label>그 날의 경기 *</label>
            <select
              className="input"
              value={gameId}
              disabled={games.length === 0}
              onChange={(e) => setGameId(e.target.value)}
            >
              <option value="">
                {gamesLoading ? '경기를 불러오는 중…' : games.length ? '경기를 선택해주세요' : '경기가 없는 날이에요'}
              </option>
              {games.map((g) => (
                <option key={g.gameId} value={g.gameId} disabled={g.status === 'CANCELED'}>
                  {g.myTeam ? '★ ' : ''}
                  {g.away.name} vs {g.home.name} · {g.stadium} {g.time} · {g.stageText ? g.stageText + ' · ' : ''}
                  {g.statusText}
                </option>
              ))}
            </select>
          </div>
        </div>
        {!gamesLoading && !gameErr && games.length === 0 && (
          <p className="err-msg">이 날은 경기가 없어서 기록할 수 없어요. 경기가 있었던 날짜를 선택해주세요.</p>
        )}
        {gameErr && <p className="err-msg">{gameErr}</p>}

        <div className="cost-head">
          <h4>얼마나 쓰셨나요?</h4>
          <span className="muted tiny">선택 입력 · 원</span>
        </div>
        <div className="three-col">
          {[
            ['ticket', '티켓'],
            ['move', '교통'],
            ['food', '식음료·기타'],
          ].map(([k, label]) => (
            <div key={k}>
              <label className="soft">{label}</label>
              <input className="input" inputMode="numeric" placeholder="0 원" value={cost[k]} onChange={setC(k)} />
            </div>
          ))}
        </div>

        <label>오늘의 이벤트 &amp; 감상 *</label>
        <textarea
          className="input note"
          placeholder="9회 말까지 믿었던 나에게 한마디…"
          value={note}
          onChange={(e) => setNote(e.target.value)}
        />

        <div className="tags">
          {TAGS.map(([v, label]) => (
            <button key={v} className={'tag' + (tag === v ? ' on' : '')} onClick={() => setTag(tag === v ? null : v)}>
              {label}
            </button>
          ))}
        </div>

        <div className="form-foot">
          <span className="muted tiny">* 필수 항목</span>
          {editId && (
            <button className="btn" onClick={resetForm}>
              취소
            </button>
          )}
          <button className="btn primary" onClick={submit} disabled={!gameId}>
            {editId ? '수정 저장 →' : '기록 확인 →'}
          </button>
        </div>
        <Status error={formErr} />
        {msg && <p className="ok-msg">{msg}</p>}
      </section>

      <section className="card dark summary">
        <h3 className="white">나의 직관 성적표</h3>
        <p className="big-point sm">{won(summary?.lossPoint)} P</p>
        <p className="soft-light tiny">손해 점수</p>
        <hr />
        <div className="two-col">
          <div>
            <p className="stat white">{summary?.visitCount ?? 0}회</p>
            <p className="soft-light tiny">직관 횟수</p>
          </div>
          <div>
            <p className="stat white">
              {summary?.wins ?? 0}승 {summary?.losses ?? 0}패
            </p>
            <p className="soft-light tiny">
              최근 기록 · {summary?.lastRecordDate ? shortDate(summary.lastRecordDate).slice(0, 5) : '-'}
            </p>
          </div>
        </div>
      </section>

      <section className="card sand cheer">
        <h2>
          패배도 기록하면
          <br />
          우리의 역사가 되니까.
        </h2>
        <p className="muted small">
          티켓값보다 오래 남을 순간을 적어보세요.
          <br />
          언젠가 이 기록도 웃으며 보겠죠?
        </p>
        <p className="eyebrow">KEEP YOUR FAN MOMENTS.</p>
      </section>

      <section className="card past">
        <div className="card-head">
          <h3>지난 직관 기록</h3>
          <span className="muted tiny">최근 {records.length}개</span>
        </div>
        <Status loading={loading} error={err} />
        {!loading && records.length === 0 && <p className="muted small">아직 기록이 없어요. 첫 직관을 남겨보세요.</p>}
        {records.map((r) => (
          <div key={r.id} className="past-row">
            <span className="pd">{shortDate(r.gameDate)}</span>
            <b className="pm">{gameText(r)}</b>
            <b className={'pr ' + (r.result === 'WIN' ? 'win' : 'lose')}>{resultText(r)}</b>
            <span className="pn muted small">{r.memo}</span>
            <b className="pc">{won(r.totalCost)}원</b>
            <button className="link-btn" onClick={() => edit(r)}>
              수정
            </button>
            <button className="link-btn" onClick={() => remove(r.id)}>
              삭제
            </button>
          </div>
        ))}
      </section>
    </div>
  )
}
