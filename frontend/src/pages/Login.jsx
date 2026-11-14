import { useState } from 'react'
import baseball from '../assets/baseball.svg'
import sparkles from '../assets/sparkles.svg'
import { api, saveSession } from '../api'

export default function Login({ onLogin }) {
  const [id, setId] = useState('')
  const [pw, setPw] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (kind) => {
    setErr('')
    if (!id.trim() || !pw) {
      setErr('아이디와 비밀번호를 입력해주세요')
      return
    }
    setBusy(true)
    try {
      const res = await api('/api/auth/' + kind, { method: 'POST', body: { loginId: id.trim(), password: pw } })
      saveSession(res.token, res.user)
      onLogin(res.user)
    } catch (e) {
      setErr(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login">
      <section className="login-brand">
        <div className="brand-top">
          <strong>본전</strong>
          <span>BONJEON CLUB</span>
        </div>
        <h2>
          야구는 낭만,
          <br />
          본전은 언제?
        </h2>
        <p className="brand-sub">
          내가 쓴 돈, 응원팀이 돌려준 행복은?
          <br />
          팬심의 가성비를 유쾌하게 확인해보세요.
        </p>
        <div className="ball-row">
          <span className="ball-label">손해 볼수록, 레벨 업</span>
          <div className="ball">
            <img src={baseball} alt="" className="ball-img" />
            <img src={sparkles} alt="" className="ball-sparkle" />
          </div>
        </div>
        <div>
          <p className="brand-slogan">
            LOSE THE GAME.
            <br />
            WIN THE DRIP.
          </p>
          <p className="brand-foot">응원은 계속된다. 본전도 언젠가는.</p>
        </div>
      </section>

      <section className="login-form-wrap">
        <form
          className="login-form"
          onSubmit={(e) => {
            e.preventDefault()
            submit('login')
          }}
        >
          <p className="eyebrow">FAN’S CHECK-IN</p>
          <h2>오늘도, 응원하러 왔나요?</h2>
          <p className="muted">로그인하고 내 팬심의 본전을 확인하세요.</p>

          <label>아이디</label>
          <input
            className="input"
            placeholder="아이디를 입력해주세요"
            value={id}
            onChange={(e) => setId(e.target.value)}
          />
          <label>비밀번호</label>
          <input
            className="input"
            type="password"
            placeholder="••••••••"
            value={pw}
            onChange={(e) => setPw(e.target.value)}
          />
          {err && <p className="err-msg">{err}</p>}
          <button className="btn primary lg" type="submit" disabled={busy}>
            로그인 →
          </button>

          <p className="muted small signup-q">아직 본전 클럽 회원이 아니신가요?</p>
          <button className="btn lg" type="button" disabled={busy} onClick={() => submit('signup')}>
            회원가입
          </button>
        </form>
      </section>
    </div>
  )
}
