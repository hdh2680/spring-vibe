# Frontend(`/app`) 폴더 구조와 파일 역할

이 문서는 `frontend/`에 생성된 React + TypeScript(Vite) 프로젝트의 파일/폴더 역할을 정리합니다.

## 한 줄 요약

- `frontend/`는 `/app/**` 아래에서 동작하는 React SPA 소스입니다.
- 개발 시 Vite dev server(`:5173`)로 실행하고, API/CSS/JS는 Spring(`:8080`)으로 프록시합니다.
- 운영에서는 React 빌드 산출물을 Spring 정적 리소스로 포함해 `:8080/app/`에서 서빙하는 형태를 목표로 합니다.

## 최상위 파일

- `frontend/package.json`
  - 프론트 의존성과 실행 스크립트가 들어있습니다.
  - 자주 쓰는 스크립트:
    - `npm run dev`: 개발 서버(Vite) 실행
    - `npm run build`: 운영 배포용 빌드(`dist/` 생성)

- `frontend/tsconfig.json`
  - TypeScript 컴파일/타입체크 옵션입니다.
  - `strict: true` 등 타입 안정성 규칙이 여기서 결정됩니다.

- `frontend/vite.config.ts`
  - Vite 개발 서버 및 빌드 설정입니다.
  - 현재 핵심:
    - dev(`serve`)일 때 `base: "/"`, build일 때 `base: "/app/"`
    - 프록시:
      - `/api` -> `http://localhost:8080`
      - `/css` -> `http://localhost:8080`
      - `/images` -> `http://localhost:8080`
      - `/js` -> `http://localhost:8080`
  - 프록시를 바꾸면: React 화면에서 호출/로딩하는 자원 경로가 바뀝니다.

- `frontend/index.html`
  - SPA 엔트리 HTML입니다(React 앱이 마운트되는 `#root` 포함).
  - 현재는 Spring 쪽 CSS/JS를 공유해서 헤더/모달 디자인을 맞춥니다.
    - `/css/brand.css`, `/css/layout_app.css`, `/css/chat.css`
    - `/js/chat.js`(AI Chat 모달 동작)

## src/ 진입점과 라우팅

- `frontend/src/main.tsx`
  - React 앱 진입점입니다.
  - `BrowserRouter basename="/app"` 설정이 여기 있습니다.
    - 즉, 브라우저 URL은 `/app/...`이지만 React Router 내부 경로는 `/...`로 매칭됩니다.

- `frontend/src/app/App.tsx`
  - 라우트 테이블을 `useRoutes`로 렌더링하는 루트 컴포넌트입니다.

- `frontend/src/app/routes.tsx`
  - React Router 라우팅 정의 파일입니다.
  - 신규 메뉴 화면을 React로 추가할 때, 결국 여기(또는 여기에서 import 하는 feature 라우트)에 경로를 추가하게 됩니다.

- `frontend/src/app/ui/AppLayout.tsx`
  - `/app` 영역의 공통 레이아웃(헤더 + 콘텐츠 + 모달 호스트)입니다.
  - 헤더/레이아웃이 깨지거나 공통 UI를 바꾸고 싶을 때 여기부터 봅니다.

## 공통 컴포넌트(shared)

- `frontend/src/shared/components/SvHeader.tsx`
  - 상단 헤더입니다.
  - 서버에서 메뉴를 받아와서 2단 mega 메뉴를 렌더링합니다.
  - 호출하는 API:
    - `/api/auth/me` (로그인 여부/권한)
    - `/api/menus/left` (메뉴 트리)
  - 로그인 상태:
    - `AI Chat` 버튼(모달 오픈 버튼 id=`chatOpen`)
    - `Logout`(Spring `/logout` POST form)
  - 비로그인 상태:
    - 메뉴 숨김
    - `Log in` 버튼만 노출(Sprint `/login`으로 이동)

- `frontend/src/shared/components/ChatModalHost.tsx`
  - AI Chat 모달의 “DOM 껍데기”만 제공합니다.
  - `chat.js`가 찾는 id/class(`chatModal`, `chatBackdrop`, `chatClose`, `chatForm` 등)를 동일하게 맞춰,
    Spring의 기존 `/js/chat.js`와 `/css/chat.css`를 React에서도 재사용할 수 있게 합니다.

## 기능(feature)

- `frontend/src/features/home/HomePage.tsx`
  - `/app` 기본 화면(placeholder)입니다.
  - 신규 메뉴를 만들면 `features/<menu>/...` 형태로 추가하는 것을 권장합니다.

## API 유틸

- `frontend/src/api/http.ts`
  - `fetch` 래퍼입니다(JSON 요청/응답 기본 처리).
  - `credentials: "include"`로 세션 쿠키 기반 인증을 유지하는 형태입니다.

## 스타일

- `frontend/src/styles/app.css`
  - `/app`에서 필요한 최소한의 보정 CSS만 둡니다.
  - 전체 톤/레이아웃은 Spring의 `/css/layout_app.css` 등을 그대로 공유하는 전략입니다.

## 어디를 고치면 뭐가 바뀌나(빠른 가이드)

- 상단 메뉴/로그인/로그아웃/채팅 버튼: `src/shared/components/SvHeader.tsx`
- 채팅 모달 UI: `src/shared/components/ChatModalHost.tsx`
- `/app` 공통 레이아웃: `src/app/ui/AppLayout.tsx`
- 라우팅 추가: `src/app/routes.tsx`
- Spring 자원(CSS/JS) 공유 여부: `index.html`, `vite.config.ts`(프록시)

