# 본전 클럽 API 명세 (초안)

> Figma 01~07 화면 기준. 회원가입 상세·모바일·상태 프레임·회원 탈퇴는 범위 밖.
> 공통 약속은 `CLAUDE.md`를 따른다: 경로 `/api/{기능}/...`, 오류는 HTTP 상태코드 + `{"message": "..."}`.

## 0. 공통

- 인증: 로그인 응답의 `token`을 `Authorization: Bearer {token}` 헤더로 보낸다. (`/api/auth/*`, `/api/teams`만 무인증)
- 인증 실패: `401 {"message": "로그인이 필요해요"}`
- 시즌: 2026 고정. 돈은 원(정수), 손해 점수는 정수 P.
- 날짜: `yyyy-MM-dd`, 시간: `HH:mm`
- 팀 코드: `LG KT SSG NC DOO KIA LOT SAM HAN KIW` (02 화면의 약어 그대로)

## 1. 도메인 정리

| 도메인 | 경로 | 쓰이는 화면 |
|---|---|---|
| auth | `/api/auth` | 01 |
| teams | `/api/teams` | 02 |
| users | `/api/users` | 02, 07 |
| home | `/api/home` | 03 |
| records | `/api/records` | 03, 04 |
| schedule | `/api/schedule` | 03, 04, 05 |
| leaderboard | `/api/leaderboard` | 03, 06 |

---

## 2. auth (01 로그인)

### POST /api/auth/signup
화면의 "회원가입" 버튼. 아이디/비밀번호만 만든다. 닉네임·응원팀은 02 화면에서 입력.
```
요청  {"loginId": "today_ball", "password": "12345678"}
응답  201 {"token": "e3b0c4...", "user": {"id": 1, "loginId": "today_ball", "nickname": null, "teamCode": null}}
오류  409 {"message": "이미 사용 중인 아이디예요"}
      400 {"message": "아이디는 영문/숫자 4~20자, 비밀번호는 8자 이상이에요"}
```

### POST /api/auth/login
```
요청  {"loginId": "today_ball", "password": "12345678"}
응답  200 {"token": "e3b0c4...", "user": {"id": 1, "loginId": "today_ball", "nickname": "오늘도직관", "teamCode": "HAN"}}
오류  401 {"message": "아이디 또는 비밀번호가 맞지 않아요"}
```
`nickname` 또는 `teamCode`가 `null`이면 프론트가 02 화면으로 보낸다.

### POST /api/auth/logout
```
응답  204
```

---

## 3. teams (02 팀 선택)

### GET /api/teams
```
응답  200 [{"code": "HAN", "name": "한화 이글스", "color": "#cf3f1f"}, ...10개]
```

---

## 4. users (02 닉네임·팀 설정, 07 내 정보)

### GET /api/users/me
07 화면 전체(프로필 카드 + 계정 설정).
```
응답  200 {
  "id": 1, "loginId": "today_ball", "nickname": "오늘도직관",
  "team": {"code": "HAN", "name": "한화 이글스"},
  "visitCount": 12,
  "level": {"lv": 7, "title": "낭만 수집가"}
}
```

### PUT /api/users/me/profile
02 화면의 "닉네임 + 응원팀 선택", 07의 이름 변경/응원팀 변경이 같은 API를 쓴다.
```
요청  {"nickname": "오늘도직관", "teamCode": "HAN"}
응답  200 (GET /api/users/me와 같은 형태)
오류  409 {"message": "이미 사용 중인 닉네임이에요"}
      400 {"message": "닉네임은 2~10자예요"}
```

### PATCH /api/users/me/password
```
요청  {"currentPassword": "12345678", "newPassword": "newpass99"}
응답  204
오류  400 {"message": "현재 비밀번호가 맞지 않아요"}
```

---

## 5. home (03 홈)

### GET /api/home
홈 화면을 한 번에 채운다. (내부적으로 records/leaderboard/schedule을 조합)
```
응답  200 {
  "report": {"teamName": "한화 이글스", "nickname": "오늘도직관", "lossPoint": 128400, "level": {"lv": 7, "title": "낭만 수집가"}},
  "season": {"totalCost": 742000, "visitCount": 12, "wins": 3, "losses": 9, "winRate": 25},
  "top10": [{"rank": 1, "nickname": "9회말또역전", "teamName": "롯데 자이언츠", "lossPoint": 284000, "me": false}, ...],
  "recentGame": {"date": "2026-11-13", "stadium": "대전", "status": "FINISHED",
                 "away": {"code": "HAN", "name": "한화 이글스", "score": 2},
                 "home": {"code": "LG", "name": "LG 트윈스", "score": 5}}
}
```
- `recentGame`은 응원팀의 가장 최근 종료 경기, 없으면 `null`.
- 기록이 하나도 없으면 `lossPoint 0`, `winRate 0`.

---

## 6. records (04 직관 기록)

### GET /api/records/summary
04 우측 "나의 직관 성적표" + 03 영수증과 같은 값.
```
응답  200 {"lossPoint": 128400, "totalCost": 742000, "visitCount": 12, "wins": 3, "losses": 9, "winRate": 25, "lastRecordDate": "2026-11-13"}
```

### GET /api/records?limit=10
"지난 직관 기록" 목록 (최신순).
```
응답  200 [{"id": 7, "gameDate": "2026-11-03", "opponent": {"code": "SAM", "name": "삼성 라이온즈"},
            "result": "WIN", "tag": "VERIFIED", "memo": "KS 1차전 직관!",
            "ticketCost": 30000, "transportCost": 4000, "foodCost": 20000, "totalCost": 54000,
            "myScore": 8, "opponentScore": 7,
            "game": {"gameId": "20261103SSKT0",
                     "away": {"code": "SAM", "name": "삼성 라이온즈", "score": 7},
                     "home": {"code": "KT", "name": "KT 위즈", "score": 8}}}]
```
- `game`은 기록과 연결된 실제 경기(원정/홈, 점수는 종료 전이면 `null`). 목록 한 줄은 "삼성 7 : 8 KT"로 그린다.
- `opponent`, `myScore`, `opponentScore`는 **내 응원팀이 뛴 경기일 때만** 채워진다(아니면 `null`).

### POST /api/records
**그 날 실제로 열린 경기 중에서만 기록할 수 있다.** 프론트는 `GET /api/schedule?date=`의 `games` 중에서 고르게 하고, 그 경기의 `gameId`를 보낸다. 경기가 없는 날은 기록 자체가 불가능하다.
```
요청  {"gameDate": "2026-11-03", "gameId": "20261103SSKT0",
       "ticketCost": 30000, "transportCost": 4000, "foodCost": 20000,
       "memo": "9회 말까지 믿었던 나에게 한마디…", "tag": "REVERSE_LOSS"}
응답  201 (GET /api/records 한 건과 같은 형태)
오류  400 {"message": "경기 날짜와 감상은 필수예요"}
      400 {"message": "경기를 선택해주세요"}              (gameId 없음)
      400 {"message": "그 날 열린 경기가 아니에요"}        (그 날짜의 일정에 없는 gameId, 경기 없는 날 포함)
      400 {"message": "취소된 경기는 기록할 수 없어요"}     (우천 취소 등)
      502 {"message": "경기 일정을 불러오지 못했어요"}      (일정 조회 실패)
```
- 필수: `gameDate`, `gameId`, `memo`. 비용 3개는 선택(없으면 0).
- 서버가 `gameId`로 일정에서 경기를 찾아 원정·홈 팀과 점수를 기록에 저장한다.
- `tag`: `WIN`(승리) `LOSE`(패배) `REVERSE_LOSS`(역전패) `BIG_WIN`(대승) `VERIFIED`(직관 인증) 또는 `null`. 기분을 남기는 용도이고 승패 판정에는 **실제 점수가 우선**한다.
- `result`(WIN/LOSE) 결정: ① 내 응원팀 경기가 종료됐으면 실제 점수로(예: KT 8:7 → `WIN`, 태그가 LOSE여도 WIN) ② 그 외(내 팀 경기가 아니거나 경기 전)이면 tag(WIN·BIG_WIN→WIN, LOSE·REVERSE_LOSS→LOSE) ③ 그래도 없으면 `null`(승패 집계 제외).

### PUT /api/records/{id}
요청/응답은 POST와 같다. 내 기록이 아니면 `404`.

### DELETE /api/records/{id}
```
응답  204
```

---

## 7. schedule (05 경기 일정, 04 상대 팀 선택, 03 최근 경기)

일정 데이터는 **전부 KBO 공식 사이트 API에서 받아온다.**(손으로 만든 데이터 없음) 아래 "KBO 소스 규칙"에 실제로 호출해 확인한 내용을 적었다.

### GET /api/schedule?date=2026-11-08
```
응답  200 {
  "date": "2026-11-08",
  "games": [
    {"gameId": "20261108KTSS0", "stage": "KS", "stageText": "한국시리즈",
     "stadium": "대구", "time": "14:00", "status": "FINISHED", "statusText": "종료",
     "away": {"code": "KT",  "name": "KT 위즈",    "score": 1},
     "home": {"code": "SAM", "name": "삼성 라이온즈", "score": 12},
     "myTeam": false}
  ]
}
```
- `status`: `SCHEDULED`(경기 전) `FINISHED`(종료) `CANCELED`(우천 취소 등). **경기 중 상태는 없다.**
- `statusText`: 화면에 그대로 쓰는 문구. "경기 전", "종료", "우천 취소"
- `stage` / `stageText`: `REGULAR` 정규시즌 · `PRESEASON` 시범경기 · `WC` 와일드카드 · `SEMI_PO` 준플레이오프 · `PO` 플레이오프 · `KS` 한국시리즈 · `ALLSTAR` 올스타전 · `INTL` 국제대회. 정규시즌은 `stageText: null`.
- `away`가 왼쪽, `home`이 오른쪽. 점수는 경기 전/취소면 `null`.
- **선발 투수 정보는 다루지 않는다.** (응답에 없고, 화면의 "선발 미정" 줄은 삭제)
- 정렬: 응원팀 경기(`myTeam: true`) 먼저, 그다음 시간순. 비로그인이면 `myTeam`은 항상 false.
- 경기가 없는 날은 `games: []` (프론트는 "경기가 없는 날이에요"를 보여준다). `date`가 없으면 오늘.

### GET /api/schedule/month?month=2026-10
"날짜 선택" 달력용. 날짜별 경기 수와 취소 여부.
```
응답  200 {"month": "2026-10", "days": [{"date": "2026-10-01", "games": 4}, {"date": "2026-10-25", "games": 0, "canceled": true}, ...]}
```
경기가 없는 날은 목록에 넣지 않는다. KBO `GetMonthSchedule`를 받아서 만든다(아래).

### KBO 소스 규칙 (2026-11-14에 직접 호출해서 확인)

공통 조건: `POST https://www.koreabaseball.com/ws/Schedule.asmx/{메서드}`, form 인코딩.
헤더는 `Content-Type: application/x-www-form-urlencoded; charset=UTF-8`, `X-Requested-With: XMLHttpRequest`, **`Referer: https://www.koreabaseball.com/Schedule/Schedule.aspx`** 가 필요하다(Referer가 없으면 HTML 오류 페이지가 온다). **쿠키는 필요 없다.**

#### srIdList = 경기 종류 (`srId`를 하나씩 호출해서 각각 무슨 경기인지 확인함)

| srId | 경기 종류 | 2026 데이터 | 확인 근거 |
|---|---|---|---|
| 0 | 정규시즌 | 3월 15 · 4월 130 · 5월 135 · 6월 125 · 7월 110 · 8월 130 · 9월 108 · 10월 40경기 | 3.28 개막~10월 중순 |
| 1 | 시범경기 | 3월 60경기 | 3.12 LG–NC 등 |
| 2 | 데이터 없음 | 없음 | 전 월 조회 0건 (미사용으로 보임) |
| 3 | 준플레이오프 | 10월 3경기 | 10.17 LG–KIA = 합쳐서 조회 시 비고 "준PO 1차전" |
| 4 | 와일드카드 | 10월 1경기 | 10.14 두산–LG = 비고 "WC 1차전" |
| 5 | 플레이오프 | 10월 5건(우천취소 1건 포함) + 11월 1경기 | 10.26 KIA–삼성 = 비고 "PO 1차전", 11.01 = "PO 5차전" |
| 6 | 데이터 없음 | 없음 | 전 월 조회 0건 |
| 7 | 한국시리즈 | 11월 5경기 | 11.03 삼성–KT = 비고 "KS 1차전" |
| 8 | 국제대회 | 3월 47 · 9월 22 · 11월 6경기 | 03.05 대만–호주, 도쿄돔 등 (팀 이름이 국가) |
| 9 | 올스타전 | 7월 1경기 | 07.11 나눔–드림 |

- 화면용 일정에는 `0, 1, 3, 4, 5, 7, 9`만 쓴다. 국제대회(8)는 KBO 팀 경기가 아니라서 뺀다.
- **`srId`를 하나씩 따로 호출해서 stage를 정한다.** (합쳐서 호출하면 비고 칸에서만 구분 가능한데, 따로 호출하면 비고가 `-`이므로 `srId`가 곧 stage)
- 처음 받은 `API 리스트.md`의 `0,9,6`은 정규시즌 + 올스타전 + (데이터 없음)이고, `3,4,5,7`은 준PO·WC·PO·KS를 합친 것이다. 즉 **정규시즌과 한국시리즈는 srId만 다르고 응답 구조는 같다.**

#### GetScheduleList — 월 단위 경기 목록
`leId=1&srIdList={srId}&seasonId=2026&gameMonth=MM&teamId=`

- 응답 `rows[].row[]`의 `{Text, Class}`. 값은 HTML 문자열이라 태그를 벗겨 쓴다.
- 칸 순서: (`day`) · `time` · `play` · 리뷰 · 하이라이트 · 중계 · 라디오 · **구장** · 비고
- 정규시즌은 하루에 최대 5경기라서 `day` 칸이 **그 날 첫 행에만** 있다(`RowSpan`). 다음 행들은 직전 날짜를 이어받는다. 포스트시즌은 하루 1경기라 매 행에 날짜가 있고, 쉬는 날은 `이동일` 행(경기 칸 없음)이라 건너뛴다.
- `play` 칸: `<span>두산</span><em><span class="lose">1</span><span>vs</span><span class="win">4</span></em><span>LG</span>` → **왼쪽이 원정, 오른쪽이 홈**. 팀 이름은 약칭(두산 LG KIA 삼성 KT 롯데 SSG NC 키움 한화)이라 팀 코드 매핑이 필요하다.
- 취소 경기: `play`에 점수 없이 `<em><span>vs</span></em>`만 있고 비고가 `우천취소`. 리뷰 링크가 없어 `gameId`도 없으므로 `날짜 + 원정 + 홈 + 시간`으로 직접 만든 키를 쓴다.
- 응답에 **없는 것**: 선발 투수, 경기 중 정보. (그래서 선발·경기 중은 화면과 API에서 뺐다)

#### GetMonthSchedule — 달력(날짜 선택)용
`leId=1&srIdList=0,1,3,4,5,7,9&seasonId=2026&gameMonth=MM` (srId를 합쳐서 한 번에 호출해도 정상 동작하는 것을 확인)

- 일~토 7칸 × 4~6주(보통 5행). 한 칸은 `<li class="dayNum">14</li>`와, 그 날 경기마다 `<a href='...gameId=20261014OBLG0...'><li>두산 <b>1 : 4</b> LG</li></a>`.
- 그 달이 아닌 칸은 `dayNum`이 비어 있다. 경기 없는 날은 `dayNum`만 있다. 우천취소 날(10/25)은 링크 없이 `dayNum`만 있어서 목록 API(`비고 = 우천취소`)와 합쳐서 `canceled`를 판단한다.
- 이 응답에는 시간·구장이 없다. 그래서 `/api/schedule?date=`는 `GetScheduleList`, `/api/schedule/month`는 `GetMonthSchedule`로 만든다.

#### gameId 읽는 법 — `20261014OBLG0`

| 자리 | 값 | 뜻 |
|---|---|---|
| 1~8 | `20261014` | 경기 날짜 yyyyMMdd |
| 9~10 | `OB` | **원정** 팀 코드 (화면에서 왼쪽 팀) |
| 11~12 | `LG` | **홈** 팀 코드 (화면에서 오른쪽 팀) |
| 13 | `0` | 더블헤더 구분. 2026 데이터 821건이 전부 `0`(단일경기). 더블헤더라면 1·2로 추정(미확인) |

팀 코드: `HH` 한화 · `HT` KIA · `KT` KT · `LG` LG · `LT` 롯데 · `NC` NC · `OB` 두산 · `SK` SSG · `SS` 삼성 · `WO` 키움. 올스타전은 `WE` 나눔 · `EA` 드림. 국제대회는 `KR` `JP` `TW` `US` `AU` 등 국가 코드.
(02 화면의 팀 약어 `HAN, KIA, LOT, SAM, DOO, SSG, KIW`와 다르므로 매핑표가 필요하다: HH→HAN, HT→KIA, LT→LOT, SS→SAM, OB→DOO, SK→SSG, WO→KIW, 나머지 LG·KT·NC는 동일)
`gameId`는 경기 고유키로 쓰고, 직관 기록과 경기를 연결할 때도 사용한다.

### JSON 파일 캐싱 (백엔드 내부 규칙)
호출마다 KBO를 때리지 않도록 **월별 JSON 파일에 저장해서 쓴다.**

- 저장 위치: `backend/data/schedule/{yyyy-MM}.json` (환경변수 `SCHEDULE_CACHE_DIR`로 변경 가능)
- 파일 내용: `{"fetchedAt": "...", "games": [ ...위 games 형식 + "date" ...], "calendar": [ ...month days... ]}`. 한 번 갱신할 때 srId마다 `GetScheduleList`(7번) + `GetMonthSchedule`(1번)을 호출해서 합쳐 저장한다.
- 갱신 규칙
  - 파일이 없으면 KBO에서 받아 저장. 지난 달은 한 번 저장하면 영구 사용.
  - 이번 달은 `fetchedAt`이 10분 넘었으면 갱신.
  - 데이터가 0건인 달(예: 11월 정규시즌)도 그대로 저장해서 반복 호출하지 않는다.
  - KBO 호출이 실패하면 **오래된 파일이라도 그대로 응답**(파일이 없으면 `502 {"message": "경기 일정을 불러오지 못했어요"}`).
- 쓰기는 임시 파일에 쓴 뒤 rename. 같은 달 동시 갱신은 `synchronized`로 한 번만.
- Render 무료 플랜은 재배포 시 파일이 사라지므로, 시작할 때 이번 달·지난달을 미리 받아 둔다.
- **오늘(2026-11-14)은 경기가 0건이다.** 한국시리즈가 11.08 5차전으로 끝났고 정규시즌 11월 데이터가 없다. 실제 API 결과가 그렇기 때문에 빈 상태로 보여주는 것이 맞다.

---

## 8. leaderboard (06 리더보드, 03 TOP 10)

### GET /api/leaderboard?nickname=&limit=
```
응답  200 {
  "items": [{"rank": 1, "nickname": "9회말또역전", "teamName": "롯데 자이언츠", "visitCount": 21, "lossPoint": 284000, "me": false}, ...],
  "me": {"rank": 7, "nickname": "오늘도직관", "teamName": "한화 이글스", "visitCount": 12, "lossPoint": 128400, "me": true}
}
```
- 손해 점수 높은 순. 동점이면 직관 횟수 많은 순, 그다음 가입 빠른 순.
- `nickname`(부분 일치)을 주면 해당 닉네임만 반환하되 `rank`는 **전체 순위 그대로**. 결과 없으면 `items: []`.
- `limit` 기본 50. 03 홈은 `limit=10`, 06은 기본값. 화면은 1~3위를 포디움, 4위부터 표로 나눠 그린다.
- `me`는 항상 내려준다(하단 고정 "MY RANK"). 기록이 없는 유저도 순위 맨 아래로 포함.

---

## 9. 백엔드 로직 (제안 — 숫자는 바꿔도 됨)

화면에 "기준 미정"이라 적혀 있어서 일단 이렇게 정했다.

- **손해 점수(lossPoint)** = 패배(`result = LOSE`) 기록마다 `5,000 + 총지출 × 10%`, 태그가 `REVERSE_LOSS`면 그 기록은 ×2. 승리·미정 기록은 0점.
- **승률** = `wins / (wins + losses) × 100` 반올림 정수. 승패 판정이 없는 기록(`result = null`)은 제외.
- **직관 횟수** = 내 기록 개수. **누적 지출** = 모든 기록의 `totalCost` 합.
- **레벨** = `lv = min(10, 1 + lossPoint ÷ 20,000)`. 이름: 1 입문 팬 · 2 새내기 · 3 응원단원 · 4 단골 · 5 열혈 팬 · 6 베테랑 · 7 낭만 수집가 · 8 불굴의 팬 · 9 전설의 팬 · 10 명예의 전당
- 닉네임·아이디는 중복 불가. 비밀번호는 BCrypt 해시(이미 spring-security-crypto가 없다면 승인받고 추가).
## 10. 확인 필요

- 아직 시작 안 한 경기의 `play` 칸 모양(점수 없음으로 추정). 조회되는 데이터가 전부 종료된 경기였다. 점수 칸이 없으면 `SCHEDULED`로 처리한다.
- 더블헤더 `gameId` 마지막 자리(1·2로 추정). 2026 데이터에는 없다.
- `srId` 2, 6은 2026에 데이터가 없어 용도를 알 수 없다(쓰지 않는다).
