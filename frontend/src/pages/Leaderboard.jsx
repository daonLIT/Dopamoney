import { useEffect, useState } from 'react'
import { api, pad, won } from '../api'
import Status from '../components/Status'

const TAILS = ['올해의 낭만왕', '팬심은 이미 우승', '팬심은 이미 우승']

export default function Leaderboard() {
  const [q, setQ] = useState('')
  const [keyword, setKeyword] = useState('')
  const [data, setData] = useState(null)
  const [err, setErr] = useState('')

  useEffect(() => {
    let alive = true
    api('/api/leaderboard' + (keyword ? '?nickname=' + encodeURIComponent(keyword) : ''))
      .then((res) => {
        if (!alive) return
        setData(res)
        setErr('')
      })
      .catch((e) => {
        if (alive) setErr(e.message)
      })
    return () => {
      alive = false
    }
  }, [keyword])

  const items = data?.items || []
  // 검색 중에는 순위 그대로 표로만 보여주고, 평소에는 1~3위 포디움
  const podium = keyword ? [] : items.filter((r) => r.rank <= 3)
  const list = keyword ? items : items.filter((r) => r.rank > 3)
  const me = data?.me

  return (
    <div>
      <form
        className="card search"
        onSubmit={(e) => {
          e.preventDefault()
          setKeyword(q.trim())
        }}
      >
        <span className="search-icon">⌕</span>
        <input placeholder="닉네임을 검색해보세요" value={q} onChange={(e) => setQ(e.target.value)} />
        <button className="btn primary" type="submit">
          검색
        </button>
      </form>

      <div className="card-head plain">
        <p className="muted">함께 울고, 같이 웃는 팬들의 명예의 전당.</p>
        <span className="muted tiny">손해 점수 높은 순</span>
      </div>

      <Status loading={!data && !err} error={err} />

      {podium.length > 0 && (
        <div className="podium">
          {podium.map((r, i) => (
            <div key={r.rank} className={'card pod' + (i === 0 ? ' dark' : '')}>
              <div className="pod-top">
                <span className="pod-rank">{pad(r.rank)}</span>
                <div>
                  <b className="pod-nick">{r.nickname}</b>
                  <p className="tiny pod-team">{r.teamName}</p>
                </div>
              </div>
              <div className="pod-bot">
                <b className="pod-pt">{won(r.lossPoint)} P</b>
                <span className="tiny pod-tail">{TAILS[i]}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {data && (
        <section className="card full-board">
          <div className="lb-row head">
            <span>순위</span>
            <span>닉네임</span>
            <span>응원팀</span>
            <span>직관 횟수</span>
            <span className="r">손해 점수</span>
          </div>
          {keyword && items.length === 0 && <p className="muted empty">‘{keyword}’ 닉네임을 찾지 못했어요.</p>}
          {!keyword && items.length === 0 && <p className="muted empty">아직 순위가 없어요.</p>}
          {list.map((r) => (
            <div key={r.rank} className={'lb-row' + (r.me ? ' me' : '')}>
              <span className={'rank' + (r.me ? ' hot' : '')}>{pad(r.rank)}</span>
              <b>
                {r.nickname}
                {r.me && '  · 나'}
              </b>
              <span className="muted small">{r.teamName}</span>
              <span className="muted small">{r.visitCount}회</span>
              <b className={'r' + (r.me ? ' hot' : '')}>{won(r.lossPoint)} P</b>
            </div>
          ))}
        </section>
      )}

      {me && (
        <div className="my-rank">
          <span className="tiny">MY RANK</span>
          <b className="mr-n">{pad(me.rank)}</b>
          <span>
            {me.nickname} &nbsp;·&nbsp; {me.teamName}
          </span>
          <b className="mr-p">{won(me.lossPoint)} P</b>
        </div>
      )}
    </div>
  )
}
