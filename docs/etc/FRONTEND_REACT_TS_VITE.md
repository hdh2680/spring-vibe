# Frontend (React + TypeScript, Vite) 병행 운영 가이드

이 문서는 `spring-vibe`에서 기존 Thymeleaf 화면은 유지하고, 앞으로 추가되는 메뉴는 React + TypeScript로 개발하는 병행 운영 방식을 정리한다.

목표는 다음과 같다.
- 기존 화면: `/login`, `/main`, `/intro` 등 Thymeleaf 기반 라우팅 유지
- 신규 화면: `/app/**` 아래는 React SPA로 제공
- 데이터 통신: Spring API는 `/api/**`로 제공하고 React는 API만 호출

---

## 1. Vite가 뭔가

Vite는 프론트엔드용 "개발 서버 + 빌드 도구"다.
- 개발 시: 로컬 개발 서버를 띄우고 코드 변경을 즉시 반영한다(HMR).
- 배포 시: 정적 파일 묶음(`index.html`, JS, CSS 등)으로 빌드해 `dist/`를 만든다.

---

## 2. 리포지토리 구조(권장)

Spring(backend)과 React(frontend)를 같은 저장소에 두되, 폴더를 분리한다.

- `frontend/`: React + TS 프로젝트(Vite)
- `src/main/java`, `src/main/resources`: 기존 Spring Boot 프로젝트

`frontend/`는 아래 구조를 권장한다.

```text
frontend/
  package.json
  vite.config.ts
  tsconfig.json
  index.html
  src/
    main.tsx
    app/
      App.tsx
      routes.tsx
    api/
      http.ts
    features/
      (신규 메뉴 단위로 폴더 분리)
    shared/
      components/
      hooks/
      utils/
    styles/
      app.css
```

설계 의도는 다음과 같다.
- `features/`: "메뉴" 혹은 "도메인" 기준으로 화면을 추가한다.
- `shared/`: 공통 컴포넌트와 유틸을 모아 중복을 줄인다.
- `api/`: API 호출 규칙을 한 곳에 모아 에러 처리와 인증 이슈를 단일화한다.

---

## 3. URL/라우팅 규칙(병행 운영 핵심)

병행 운영에서 가장 중요한 규칙은 "React는 `/app` 아래에서만 동작"하게 하는 것이다.

- 기존 Thymeleaf 라우트는 그대로 유지한다.
- 신규 메뉴는 링크를 `/app/...`로 건다.
- React Router는 `/app`을 basename으로 사용한다.

예시(개념):
- 신규 메뉴: `/app/dev-search`, `/app/youtube/analysis`
- API: `/api/dev-search`, `/api/youtube-comment/...`

---

## 4. 개발 모드(로컬) 동작 방식

개발할 때는 Spring과 Vite를 각각 실행한다.
- Spring Boot: `http://localhost:8080`
- Vite dev server: `http://localhost:5173`

권장 흐름은 다음과 같다.
- 화면 개발은 `5173`에서 한다.
- API 호출은 Vite 프록시를 통해 `/api`를 `8080`으로 전달한다.

이 방식을 쓰면 다음 이점이 있다.
- CORS 설정을 최소화할 수 있다.
- 세션 쿠키 기반 로그인도 개발 과정에서 유지하기 쉽다.

주의할 점이 있다.
- 브라우저 주소창에 `http://localhost:8080/app/...`로 접근하면, 아직 React 빌드 산출물이 없을 수 있다.
- 개발 중에는 의도적으로 `5173`에서 화면을 보고, `8080`은 API 서버로 본다.

실행(예시):
- `cd frontend`
- `npm install`
- `npm run dev` (http://localhost:5173/app)

---

## 5. 배포 모드(운영) 동작 방식

운영에서는 React를 빌드한 결과물을 Spring이 정적 파일로 서빙하도록 묶는다.

권장 배포 목표는 다음이다.
- 운영 URL: `http://서버/app/` 에서 React가 로드된다.
- `/api/**`는 같은 도메인에서 Spring이 처리한다.

즉, 운영에서 "React 서버"는 따로 없고, Spring이 정적 파일을 서빙한다.

빌드(예시):
- `cd frontend`
- `npm run build` (산출물: `frontend/dist`)

---

## 6. SPA 딥링크(새로고침 404) 처리

SPA는 `/app/reports` 같은 경로를 클라이언트 라우터가 처리한다.
하지만 사용자가 직접 해당 URL로 접속하거나 새로고침하면, 서버는 파일을 찾으려다가 404를 낼 수 있다.

그래서 Spring에 아래 동작이 필요하다.
- `/app/**` 요청에서 "정적 파일이 아닌 경우"는 `/app/index.html`로 forward
- forward는 브라우저 URL을 바꾸지 않고 서버 내부에서 리소스를 반환한다.
- redirect는 브라우저가 다른 URL로 이동한다.

병행 운영에서는 redirect로 `5173`에 보내는 방식을 운영에 쓰지 않는다.
운영은 `8080` 한 곳에서 `/app`도 함께 서빙하는 구조가 기준이다.

---

## 7. API 호출 규칙(React 쪽)

React에서 API를 호출할 때는 다음을 권장한다.
- API base는 `/api`로 통일한다.
- 공통 HTTP 클라이언트(`api/http.ts`)에서 다음을 표준화한다.
  - 기본 헤더(JSON)
  - 에러 응답 처리
  - 인증 실패(401/403) 시 공통 동작

세션 기반 로그인인 경우 다음이 포인트다.
- 같은 오리진(운영)에서는 쿠키가 자동으로 붙는다.
- 개발(5173)에서도 프록시를 쓰면 쿠키/세션을 유지하기 쉬워진다.

---

## 8. 메뉴 추가 방식(권장 운영 룰)

"앞으로 추가되는 메뉴는 React로"를 운영 규칙으로 만들려면, 팀 내 합의를 아래처럼 박아두는 게 좋다.

- 신규 메뉴는 `/app/...`로만 추가한다.
- Thymeleaf 템플릿에 신규 화면을 만들지 않는다(레거시 유지보수 제외).
- 메뉴 링크는 서버 레이아웃(Thymeleaf)에서 `/app/...`로 연결한다.
- 공통 레이아웃/네비게이션을 React로 옮길지 여부는 단계적으로 결정한다.

---

## 9. 체크리스트

React 병행 운영에서 자주 터지는 항목들이다.
- React build base 경로가 `/app/` 기준으로 맞는가
- Spring이 `static/app/` 아래 정적 파일을 정상 서빙하는가
- `/app/*` 딥링크 새로고침이 404가 아닌가
- `/api/**` 호출이 개발/운영 모두에서 동일한 방식으로 되는가
- 로그인 정책을 `/api/**`에 적용할 계획이 있는가
