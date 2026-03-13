# 패치노트

## 시작하기 앞서

**첫 바이브 코딩 일지**

나는 이 프로젝트에서 직접 코드를 작성하기보다
AI에게 작업을 지시하고 결과를 검토하는 **설계자이자 관리자 역할**을 수행한다.

프로젝트의 규칙과 설계를 문서로 정의하고
AI는 해당 문서를 기반으로 기능을 구현하며 작업 결과를 보고한다.
나는 그 결과를 검토하고 승인하거나 수정 지시를 한다.

다음 문서 구조를 기준으로 프로젝트를 관리한다.

---

### README.md
프로젝트 개요, 실행 방법, 주요 문서 위치를 안내하는 **프로젝트 입구 문서**

---

### PROJECT_CONTEXT.md
AI가 작업 절차와 규칙을 이해하고 따르도록 정리한 **AI 작업 가이드 문서**

---

### docs/PRD.md
프로젝트 목적, 사용자, 주요 기능 등을 정의한 **기획 문서**

---

### docs/DATABASE_SCHEMA.md
데이터베이스 테이블 구조와 관계를 정리한 **DB 스키마 문서**

---

### docs/ARCHITECTURE.md
시스템 아키텍처와 패키지 구조를 설명하는 **설계 문서**

---

### docs/CODING_RULE.md
코드 스타일, 레이어 규칙, 네이밍 규칙 등을 정의한 **코딩 규칙 문서**

---

### docs/FUNCTIONS.md
AI가 구현한 기능과 API 정보를 정리한 **기능 요약 문서**

---

### docs/TODOLIST.md
작업 목록과 진행 상태를 관리하는 **작업 관리 문서**

---

### docs/aiwork/{작업기록}
AI가 수행한 작업과 변경 내용을 기록하는 **작업 로그 디렉토리**

---

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

### PROJECT_CONTEXT.md 추가
- 해당 문서가 있을 시 AI가 해당 규칙 순서대로 일을 진행한다고 한다.

### view template 구조 수정 및 규칙 추가
- templates/html/... 에 하위에 생성한다.