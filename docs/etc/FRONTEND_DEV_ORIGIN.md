# 로컬 개발: Spring 메뉴에서 `/app/**`를 Vite(5173)로 열기

로컬 개발 환경에서는 React 화면은 Vite dev server(`http://localhost:5173`)에서 띄우고, Spring은 API/Thymeleaf 서버(`http://localhost:8080`)로 두는 방식이 일반적입니다.

문제:
- Spring(8080) 화면에서 메뉴를 클릭했을 때 `/app/**`로 이동하면, 운영처럼 `8080/app/**`로 붙습니다.
- 아직 React 빌드 산출물이 `static/app/`에 없으면 `app/index.html`을 찾지 못해 404가 발생할 수 있습니다.

해결:
- **개발할 때만** Spring 메뉴의 `/app/**` 링크를 `5173`으로 보내도록 설정할 수 있습니다.

## 설정 방법

Spring 설정 키:
- `app.frontend.dev-origin`

환경변수로 주입:
- `APP_FRONTEND_DEV_ORIGIN=http://localhost:5173`

PowerShell 예시(현재 터미널 세션만):
```powershell
$env:APP_FRONTEND_DEV_ORIGIN="http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

## 동작 규칙

- 메뉴 `path`가 `/app` 또는 `/app/...`로 시작하는 경우에만
  - `dev-origin + path`로 링크를 바꿉니다.
- 그 외 메뉴(`/admin/**`, `/users/**` 등)는
  - 항상 Spring(8080) 라우트를 그대로 탑니다.

## 주의: 메뉴 `path`는 반드시 절대 경로로 저장

메뉴의 `path`는 반드시 `/app/labs/react-ts-docs`처럼 **슬래시(`/`)로 시작하는 절대 경로**여야 합니다.

- OK: `/app/labs/react-ts-docs`
- NG: `app/labs/react-ts-docs`

슬래시가 없으면 브라우저가 상대경로로 처리해서,
예를 들어 현재 페이지가 `http://localhost:8080/users/amazonProduct/list`일 때
`app/labs/react-ts-docs`를 클릭하면
`http://localhost:8080/users/amazonProduct/app/labs/react-ts-docs` 같은 잘못된 URL로 붙습니다.

