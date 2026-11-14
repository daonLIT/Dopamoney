import { useEffect, useState } from 'react'
import baseball from '../assets/baseball.svg'
import sparkles from '../assets/sparkles.svg'
import { api, pad, shortDate, won } from '../api'
import Status from '../components/Status'

const STATUS_TEXT = { FINISHED: '종료', SCHEDULED: '경기 전', CANCELED: '우천 취소' }

export default function Home({ go }) {
  const [data, setData] = useState(null)
  const [err, setErr] = useState('')

  useEffect(() => {
    api('/api/home')
      .then(setData)
      .catch((e) => setErr(e.message))
  }, [])

  if (!data) return <Status loading={!err} error={err} />
  const { report, season, top10, recentGame: g } = data
  const mine = (name) => !!report.teamName && report.teamName === name

  return (
    <div className="grid-home">
      <section className="card dark report">
        <div className="report-text">
          <p className="soft-light">
            {report.teamName} &nbsp;/&nbsp; {report.nickname} 님의 시즌 리포트
          </p>
          <h2>
            내 지갑은 아직,
            <br />
            연장전 중.
          </h2>
          <p className="soft-light small">손해 점수</p>
          <p className="big-point">
            {won(report.lossPoint)} <small>P</small>
          </p>
        </div>
        <div className="report-ball">
          <div className="ball big">
            <img src={baseball} alt="" className="ball-img" />
            <img src={sparkles} alt="" className="ball-sparkle" />
          </div>
          <span className="level-badge">
            LV. {pad(report.level.lv)} &nbsp; {report.level.title}
          </span>
        </div>
      </section>

      <section className="card receipt">
        <h3>이번 시즌, 내 팬심 영수증</h3>
        <p className="muted small">누적 지출</p>
        <p className="receipt-sum">{won(season.totalCost)}원</p>
        <hr />
        <div className="two-col">
          <div>
            <p className="muted tiny">직관 기록</p>
            <p className="stat">{season.visitCount}회</p>
          </div>
          <div>
            <p className="muted tiny">직관 승률</p>
            <p className="stat">{season.winRate}%</p>
          </div>
        </div>
        <p className="muted tiny">
          {season.wins}승 {season.losses}패 · 직관 기록 기준
        </p>
      </section>

      <section className="card top10">
        <div className="card-head">
          <h3>이번 시즌 억울함 TOP 10</h3>
          <button className="link-btn" onClick={() => go('leaderboard')}>
            전체 리더보드 보기 ↗
          </button>
        </div>
        <div className="rows">
          <div className="row head">
            <span>순위</span>
            <span>닉네임 / 응원팀</span>
            <span className="r">손해 점수</span>
          </div>
          {top10.length === 0 && <p className="muted small">아직 순위가 없어요.</p>}
          {top10.map((r) => (
            <div key={r.rank} className={'row' + (r.me ? ' me' : '')}>
              <span className={'rank' + (r.rank <= 3 || r.me ? ' hot' : '')}>{pad(r.rank)}</span>
              <span>
                <b>{r.nickname}</b>
                {r.me && <b>&nbsp; 나</b>}
                <em>{r.teamName}</em>
              </span>
              <span className={'r pts' + (r.me ? ' hot' : '')}>{won(r.lossPoint)} P</span>
            </div>
          ))}
        </div>
      </section>

      <section className="card recent">
        <div className="card-head">
          <h3>최근 응원팀 경기</h3>
          {g && <span className="pill">{STATUS_TEXT[g.status] || g.status}</span>}
        </div>
        {g ? (
          <>
            <p className="muted tiny">
              {shortDate(g.date)} · {g.stadium}
            </p>
            <div className="score-line">
              <b className={mine(g.away.name) ? 'hot' : ''}>{g.away.name.split(' ')[0]}</b>
              <span>
                {g.away.score ?? '-'} : {g.home.score ?? '-'}
              </span>
              <b className={mine(g.home.name) ? 'hot' : ''}>{g.home.name.split(' ')[0]}</b>
            </div>
          </>
        ) : (
          <p className="muted small">최근 종료된 경기가 없어요.</p>
        )}
        <hr />
        <p className="recent-msg">져도 또 보러 가는 게 팬이니까.</p>
        <p className="muted small">다음 경기는, 진짜 다를지도 몰라요.</p>
      </section>

      <section className="card sand cta">
        <h2>
          오늘의 직관,
          <br />
          기억이 생생할 때 남겨요.
        </h2>
        <p className="muted small">티켓값부터 9회 말의 한숨까지.</p>
        <button className="btn primary" onClick={() => go('record')}>
          +&nbsp; 직관 기록 남기기
        </button>
      </section>
    </div>
  )
}
