# Frontend `/app` 구현 관점 실행 흐름

이 문서는 “화면이 어떻게 뜨는가” 수준이 아니라, **빌드부터 서빙, Spring forward, React 마운트/라우팅/AppLayout/SvHeader 동작까지** 실제 실행 흐름을 정리합니다.

## 추가 자료: 실행 흐름 다이어그램(이미지)

아래 이미지는 `/app/**` 화면이 뜨는 큰 흐름(브라우저 요청, Spring 정적/forward, React 마운트, 라우팅, 공통 레이아웃, API 호출)을 한 장으로 요약한 것입니다.

![React /app screen flow diagram](/images/docs/react_app_screen_flow.svg)

---

## 1. 빌드(Vite) 흐름

1. 개발자는 `frontend/`에서 `npm run build`를 실행합니다.
2. Vite는 [vite.config.ts](/C:/workspace/java_lec/spring-vibe/frontend/vite.config.ts)의 설정을 읽습니다.
3. build 모드에서는 `base: "/app/"`로 동작합니다.
   - 결과적으로 `dist/index.html`과 번들 파일들이 “`/app/` 아래에서 호스팅된다”는 가정으로 생성됩니다.
   - 일반적으로 JS/CSS 번들은 `/app/assets/...` 경로를 기준으로 로드됩니다.
4. 산출물은 `frontend/dist/`에 생성됩니다.

운영 배포에서는 이 `dist/` 내용을 Spring 정적 리소스 경로(예: `src/main/resources/static/app/`)로 포함시키는 형태를 전제로 합니다.

---

## 2. Spring(서버) 요청 처리 흐름: `/app/**`

운영에서 브라우저가 `GET /app/...`로 요청하면 Spring은 다음 순서로 처리합니다.

1. 정적 파일 서빙
   - `src/main/resources/static/app/index.html`
   - `src/main/resources/static/app/assets/*` (번들 JS/CSS 등)
   - 즉, 실제 파일이 있으면 그대로 정적 파일로 응답됩니다.
2. SPA 딥링크 forward
   - `GET /app/report/123` 같은 “파일이 아닌 경로”는 Spring 컨트롤러가 `forward:/app/index.html`로 보냅니다.
   - 이때 브라우저 주소는 `/app/report/123` 그대로 유지되고, **서버 내부에서만** `index.html`을 반환합니다.

이 forward 매핑은 다음 파일에 있습니다.
- [AppSpaForwardController.java](/C:/workspace/java_lec/spring-vibe/src/main/java/springVibe/dev/common/controller/AppSpaForwardController.java)

---

## 3. 브라우저 로딩 흐름: `index.html -> React`

브라우저가 `/app`(또는 `/app/...`)에 접근하면 아래 순서로 실행됩니다.

1. 브라우저가 `GET /app` 요청
2. Spring이 `index.html`을 반환(정적 파일이 있거나, 딥링크면 forward로 반환)
3. 브라우저가 `index.html`을 파싱하면서 리소스를 로드
   - `index.html`에서 로드하는 CSS/JS
     - `/css/brand.css`, `/css/layout_app.css`, `/css/chat.css` (Spring CSS 공유)
     - `/js/chat.js` (Spring의 chat modal 구동 스크립트)
   - React 번들(운영): `/app/assets/...` (Vite build 산출물)
   - React 소스(개발): `/src/main.tsx` (Vite dev server가 모듈로 제공)
4. React 진입점 실행: [main.tsx](/C:/workspace/java_lec/spring-vibe/frontend/src/main.tsx)
   - `BrowserRouter basename="/app"`으로 라우터 시작
5. 라우팅 적용: [App.tsx](/C:/workspace/java_lec/spring-vibe/frontend/src/app/App.tsx) -> [routes.tsx](/C:/workspace/java_lec/spring-vibe/frontend/src/app/routes.tsx)
6. 공통 레이아웃 렌더링: [AppLayout.tsx](/C:/workspace/java_lec/spring-vibe/frontend/src/app/ui/AppLayout.tsx)
   - 상단 헤더 렌더링: `SvHeader`
   - 페이지 컨텐츠 렌더링: `<Outlet />`
   - 채팅 모달 DOM 호스트 렌더링: `ChatModalHost`

---

## 4. 헤더(메뉴/로그인) 동작 흐름: SvHeader

헤더는 “서버가 HTML에 주입한 메뉴”를 쓰지 않고, API로 상태를 가져옵니다.

1. 컴포넌트 마운트 시(effect) 아래 API를 호출합니다.
   - `GET /api/auth/me`: 로그인 여부/username/authorities
   - `GET /api/menus/left`: 상단 메뉴 트리
2. 비로그인(`authenticated=false`)이면:
   - 상단 메뉴는 렌더링하지 않음(게스트 fallback 없음)
   - `Log in` 버튼만 노출(Sprint `/login` 이동)
3. 로그인(`authenticated=true`)이면:
   - 메뉴 트리를 2단 mega menu 구조로 렌더링
   - `AI Chat` 버튼(모달 오픈 버튼 id=`chatOpen`) 노출
   - `Logout`(Spring `/logout` POST form) 노출

구현 파일:
- [SvHeader.tsx](/C:/workspace/java_lec/spring-vibe/frontend/src/shared/components/SvHeader.tsx)
- 메뉴 API: [MenuApiController.java](/C:/workspace/java_lec/spring-vibe/src/main/java/springVibe/dev/common/api/MenuApiController.java)
- 내 정보 API: [AuthApiController.java](/C:/workspace/java_lec/spring-vibe/src/main/java/springVibe/dev/common/api/AuthApiController.java)

---

## 5. 메뉴 클릭 시 이동 규칙

메뉴 데이터의 `path`는 서버 기준 절대 경로로 들어옵니다(예: `/app/report`, `/users/devSearch/list`).

- `path`가 `/app/`로 시작하면:
  - React Router 내부 이동으로 처리합니다.
  - `basename="/app"`이므로 `/app/xxx`는 React 내부에서는 `/xxx`로 매칭됩니다.
- 그 외 경로면:
  - Spring 서버 라우트로 이동(전체 페이지 로드)합니다.

---

## 6. AI Chat 모달 동작 흐름

React는 모달 “기능”을 직접 구현하지 않고, Spring이 이미 갖고 있는 `chat.js`/`chat.css`를 재사용합니다.

1. `SvHeader`의 `AI Chat` 버튼은 `id="chatOpen"`을 갖습니다.
2. `ChatModalHost`는 `chat.js`가 찾는 모달 DOM id/class를 그대로 제공합니다.
   - `chatModal`, `chatBackdrop`, `chatClose`, `chatClear`, `chatForm`, `chatMessage`, `chatSend`, `chatLog`, `chatHealth`
3. 브라우저에서 `/js/chat.js`가 로드되면 위 DOM에 이벤트를 바인딩하고,
   `AI Chat` 클릭 시 모달을 열고 `/api/chat/**`를 호출합니다.

구현 파일:
- [ChatModalHost.tsx](/C:/workspace/java_lec/spring-vibe/frontend/src/shared/components/ChatModalHost.tsx)
- `index.html`의 로드: [frontend/index.html](/C:/workspace/java_lec/spring-vibe/frontend/index.html)

---

## 7. 개발(Dev) vs 운영(Prod) 차이

- 개발(Vite): `http://localhost:5173/app`
  - dev 모드에서는 `base: "/"`로 동작합니다.
  - Vite가 `/api`, `/css`, `/images`, `/js`를 `http://localhost:8080`으로 프록시합니다.
- 운영(Spring): `http://localhost:8080/app`
  - build 모드에서는 `base: "/app/"`로 빌드되어, `/app/assets/...` 기준으로 번들이 로드됩니다.
  - `/app/**`는 정적 파일 + SPA forward 조합으로 서빙됩니다.
