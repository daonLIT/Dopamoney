import homeOn from '../assets/home-on.svg'
import homeOff from '../assets/home-off.svg'
import recordOn from '../assets/record-on.svg'
import recordOff from '../assets/record-off.svg'
import scheduleOn from '../assets/schedule-on.svg'
import scheduleOff from '../assets/schedule-off.svg'
import trophyOn from '../assets/trophy-on.svg'
import trophyOff from '../assets/trophy-off.svg'
import userOn from '../assets/user-on.svg'
import userOff from '../assets/user-off.svg'

const NAV = [
  { key: 'home', label: '홈', on: homeOn, off: homeOff },
  { key: 'record', label: '직관 기록', on: recordOn, off: recordOff },
  { key: 'schedule', label: '경기 일정', on: scheduleOn, off: scheduleOff },
  { key: 'leaderboard', label: '리더보드', on: trophyOn, off: trophyOff },
  { key: 'myinfo', label: '내 정보', on: userOn, off: userOff },
]

export default function Layout({ page, go, user, eyebrow, title, children }) {
  return (
    <div className="shell">
      <header className="nav">
        <div className="nav-inner">
          <button className="brand" onClick={() => go('home')}>
            본전<span>CLUB</span>
          </button>
          <nav className="nav-links">
            {NAV.map((n) => (
              <button
                key={n.key}
                className={'nav-link' + (page === n.key ? ' active' : '')}
                onClick={() => go(n.key)}
              >
                <img src={page === n.key ? n.on : n.off} alt="" />
                <span>{n.label}</span>
              </button>
            ))}
          </nav>
          <button className="profile-chip" onClick={() => go('myinfo')}>
            {user.nickname} ↗
          </button>
        </div>
      </header>

      <main className="container">
        <div className="page-heading">
          <div>
            <p className="eyebrow">{eyebrow}</p>
            <h1>{title}</h1>
          </div>
          <p className="muted small">2026 시즌</p>
        </div>
        {children}
      </main>

      <footer className="container footer">
        <span>BONJEON CLUB &nbsp; / &nbsp; 응원은 계속된다.</span>
        <span>계산 기준은 서비스 정책에 따라 달라질 수 있어요.</span>
      </footer>
    </div>
  )
}
