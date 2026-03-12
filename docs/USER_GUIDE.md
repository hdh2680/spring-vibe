# 사용자 가이드 (USER_GUIDE)

## 0. 정의
- `/README.md` 기준 플레이스홀더 사용

## 1. 로그인
- 화면: `GET /login`
- 처리: `POST /login` (파라미터: `username`, `password`)
- 실패: `/login?error`
- 로그아웃: `/login?logout`
- 로그인 성공/실패는 서버에서 자동으로 로그인 이력(`login_logs`)에 저장됨

## 2. 메인 화면
- URL: `/`
- 로그인 성공 시 진입 (인증 필요)
- 로그아웃 버튼 제공 (`POST /logout`)

## 3. 메뉴/기능 (예정)
- 대시보드: DB 상태/데이터 품질 시각화
- 테이블 탐색: 컬럼, 관계, 레코드 수 확인
- 데이터 품질: Null, 중복, 범위 벗어남 탐지
- 리포트 생성: PDF/Excel/CSV, 스케줄링 가능
