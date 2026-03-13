# 개요
본 문서는 프로젝트의 목적, 사용자, 핵심 기능 등 제품 요구사항을 정의한 문서입니다. 

**아키텍처 및 패키지 구조**는 `/docs/ARCHITECTURE.md` 참고

# 권한
- ROLE_ADMIN : 관리자
- ROLE_USER : 사용자

# 로그인
- 패키지 : src/main/java/basemarkdown/dev/common/
- 매핑 : /login
- 화면 : templates/html/login.html

## 1. 로그인로그
- 패키지 : src/main/java/basemarkdown/dev/common/
- 관련테이블
  - login_logs : 사용자 로그인 이력 로그테이블

# 메뉴
## 1. 사용자관리
- 패키지 : src/main/java/basemarkdown/dev/admin/users/
- 매핑 : /admin/users
- 화면 : templates/admin/users/
- 관련테이블 
  - users : 사용자
  - roles : 권한
  - user_roles : 사용자권한
- 기능 
  - 사용자목록조회 : /list
  - 사용자상세조회 : /view
  - 사용자등록 : /regist
  - 사용자수정(사용자 상태컬럼 status 수정, 권한부여) : /regist

## 2. 메뉴관리
- 패키지 : src/main/java/basemarkdown/dev/admin/menus/
- 매핑 : /admin/menus
- 화면 : templates/admin/menus/
- 관련테이블
  - menus : 메뉴
- 기능
  - 메뉴목록조회 : /list
  - 메뉴상세조회 : /view
  - 메뉴등록 : /regist
  - 메뉴수정 : /regist
  - 메뉴삭제

## 3. 권한메뉴관리
- 패키지 : src/main/java/basemarkdown/dev/admin/roleMenus/
- 매핑 : /admin/roleMenus
- 화면 : templates/admin/roleMenus/
- 관련테이블
  - menus : 메뉴
  - roles : 권한
  - role_menus : 권한 별 메뉴
- 기능
  - 권한메뉴관리 : /manage
    - 좌측 권한목록, 우측 메뉴목록 조회하여 체크박스로 메뉴권한부여