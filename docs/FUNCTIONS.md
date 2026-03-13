# 기능 정의서 (FUNCTIONS)

## 0. 정의
- `/README.md` 기준 플레이스홀더 사용

## 1. 인증/권한
- 로그인: `GET /login` (Thymeleaf), `POST /login` (Spring Security form login)
- 로그아웃: `POST /logout`
- 로그인 성공: 계정 권한(`user_roles`) 기반으로 접근 가능한 첫 메뉴로 이동
- 역할(Role): `roles`, `user_roles` 기반으로 `ROLE_*` 권한 부여
- 비밀번호: SHA-256 해시(hex) 비교
- 로그인 이력: 로그인 성공/실패 시 `login_logs` 테이블에 적재 (IP, User-Agent 포함)

## 2. 관리자(Admin)

### 2.1 사용자관리
- Base: `/admin/users`
- 화면/기능
  - 목록: `GET /admin/users/list`
  - 상세: `GET /admin/users/view?id={id}`
  - 등록: `GET /admin/users/regist`, `POST /admin/users/regist`
  - 수정(상태/권한 포함): `GET /admin/users/regist?id={id}`, `POST /admin/users/regist`
- 관련 테이블: `users`, `roles`, `user_roles`

### 2.2 메뉴관리
- Base: `/admin/menus`
- 화면/기능
  - 목록: `GET /admin/menus/list`
  - 상세: `GET /admin/menus/view?id={id}`
  - 등록: `GET /admin/menus/regist`, `POST /admin/menus/regist`
  - 수정: `GET /admin/menus/regist?id={id}`, `POST /admin/menus/regist`
  - 삭제: `POST /admin/menus/delete`
- 관련 테이블: `menus`

### 2.3 권한메뉴관리
- Base: `/admin/roleMenus`
- 화면/기능
  - 권한별 메뉴 부여/해제: `GET /admin/roleMenus/manage?roleId={roleId}`, `POST /admin/roleMenus/manage`
- 관련 테이블: `roles`, `menus`, `role_menus`
- 비고: `ROLE_ADMIN`은 이 화면에서 수정 불가(서버에서 차단)

## 3. 데이터 품질 관리 (예정)
- 테이블 구조 조회, 통계, 이상치 탐지, 리포트 생성

## 4. 시스템 관리 (예정)
- 사용자, 스케줄링, 알람

## 5. 패키지 구조
- `{{ROOT_PACKAGE}}.system.config`
- `{{ROOT_PACKAGE}}.system.exception`
- `{{ROOT_PACKAGE}}.system.security`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.controller`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.service`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.repository`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.mapper`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.domain`
