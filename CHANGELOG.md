# 패치노트

## 2026-03-12

### 프로젝트 생성
- README.md
- docs/ARCHITECTURE.md
- docs/CODING_RULE.md
- docs/FUNCTIONS.md
- docs/TODOLIST.md

스켈레톤 프로젝트 생성완료

### 로그 적재 규칙 추가
AI일 시키기 -> 일을 마친 후 규칙대로 일을 실행하였는지 -> 자신이 한 일을 로그형식으로 남기기
README.md에 AI Work Log Rule로 로그남기도록 추가

### 로그인 기능 추가
- 로그인하여 메인화면으로 이동기능 추가
- 로그인 성공 시 로그인로그 적재하도록 추가

### Thymeleaf를 사용한 레이아웃 분기처리
- th:replace="${contentTemplate} :: content" 로 다른 템플릿의 fragment를 끼워 넣는 방식 처리

---

## 2026-03-13

### 문서 PRD, DATABASE_SCHEMA 추가
- docs/PRD.md
  : 프로젝트에서 만들 메뉴의 필요한 정보를 기술하여 AI가 CODING_RULE.md의 규칙을 지켜가며 기술된 내용대로 코딩한다.
- docs/DATABASE_SCHEMA.md
  : 실제 업무에서 DB설계 후 테이블 생성하게 됬을때 해당 문서를 AI한테 작성 요청하여 DB탐색을 하지 않고 해당문서로 정보를 제공한다.

### 소스정리 및 관리자/사용자 패키징
- 스켈레톤 소스 정리
- /admin/~~~ 의 관리자 패키징과 /users/~~~ 로 패키징 정리
- 디자인 테마 밝게 수정

### Controller 네이밍규칙 추가
- /admin/users -> AdminUsersController의 규칙으로 생성하여 UsersController 의 규칙을 명시해줌
CODING_RULE.md > 1.2.1 Controller 네이밍(리소스 기준)

### 로그인사용자의 권한에 따라 메뉴목록이 레이아웃에 적용되도록 요청

### 관리자메뉴 3개 구현
- 사용자관리
- 메뉴관리
- 권한메뉴관리