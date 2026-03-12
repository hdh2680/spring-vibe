# 기능 정의서 (FUNCTIONS)

## 0. 정의
- `/README.md` 기준 플레이스홀더 사용

## 1. 인증/권한
- 로그인: `GET /login` (Thymeleaf), `POST /login` (Spring Security form login)
- 로그아웃: `POST /logout`
- 로그인 성공: `/` 이동
- 역할(Role): `roles`, `user_roles` 기반으로 `ROLE_*` 권한 부여
- 비밀번호: SHA-256 해시(hex) 비교
- 로그인 이력: 로그인 성공/실패 시 `login_logs` 테이블에 적재 (IP, User-Agent 포함)

## 2. 데이터 품질 관리 (예정)
- 테이블 구조 조회, 통계, 이상치 탐지, 리포트 생성

## 3. 시스템 관리 (예정)
- 사용자, 스케줄링, 알람

## 4. 패키지 구조
- `{{ROOT_PACKAGE}}.system.config`
- `{{ROOT_PACKAGE}}.system.exception`
- `{{ROOT_PACKAGE}}.system.security`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.controller`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.service`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.repository`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.mapper`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.domain`
