# AI MD 프로젝트 기초 생성

## 0. 변수 정의
이 문서에서 사용되는 플레이스홀더:
- `{{ROOT_PACKAGE}}` = basemarkdown
- `{{DEVELOP_PACKAGE}}` = dev

---

## 1. 프로젝트 개요
본 프로젝트는 MARKDOWN으로 AI를 활용하여 기본적인 프로젝트를 생성하는데 의미를 둠.

---

## 2. 기술 스택
- **Backend**: Java 17, Spring Boot 3.x
- **ORM**: Spring Data JPA (Entity 관리), MyBatis (복잡한 쿼리/통계)
- **Database**: MySQL 8.0+
- **Frontend**: Thymeleaf, JavaScript, CSS3
- **DevOps/Monitoring**: Spring Boot Actuator, Swagger UI (OpenAPI 3)
- **빌드 도구**: Maven Wrapper (`mvnw`) 지원

---

## 3. Git 형상관리 정보
- **Repository URL**: `https://github.com/hdh2680/basemarkdown.git`

branch
- master

---

## 4. 프로젝트 구조 및 아키텍처
기능별 도메인 구조(Domain-driven)를 따릅니다.

- `system/`: 인증, 트랜잭션, 보안 등 공통 시스템 설정
- `{{DEVELOP_PACKAGE}}`: 데이터 품질 관리 및 DB 분석 관련 비즈니스 로직
  - `controller/`: REST API 엔드포인트
  - `service/`: 비즈니스 로직 및 트랜잭션 단위
  - `repository/`: JPA 인터페이스
  - `mapper/`: MyBatis SQL 매핑
  - `domain/`: 엔티티(Entity) 및 DTO

**아키텍처 및 패키지 구조**는 `/docs/ARCHITECTURE.md` 참고

---

## 5. 실행 방법

### 5.0 사전 준비
- Java 17 설치
- MySQL 8 실행 및 DB 생성(예시):
```sql
CREATE DATABASE basemarkdown CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
- DB 계정 정보 설정:
  - `src/main/resources/application.yml`의 `spring.datasource.username/password` 수정

### 5.1 프로젝트 빌드
```bash
mvn clean install
```
(Maven 미설치 시: `./mvnw clean install` — Windows는 `mvnw.cmd`)

### 5.2 백엔드 실행
```bash
mvn spring-boot:run
```
(Maven 미설치 시: `./mvnw spring-boot:run` — Windows는 `mvnw.cmd`)

### 5.3 Swagger UI 확인
http://localhost:8080/swagger-ui.html  
(또는 http://localhost:8080/swagger-ui/index.html)

### 5.4 Actuator 확인
http://localhost:8080/actuator/health

### 5.5 샘플 페이지
http://localhost:8080/login

### 5.6 Main
http://localhost:8080/

## 6. 참고 문서
- `/docs/PRD.md` : 프로젝트의 목적, 사용자, 핵심 기능 등 **제품 요구사항을 정의한 문서**
- `/docs/DATABASE_SCHEMA.md` : 시스템에서 사용하는 **데이터베이스 테이블 구조와 컬럼 정보를 정리한 문서**
- `/docs/ARCHITECTURE.md` : 시스템의 **전체 아키텍처 구조와 패키지 설계 방식을 설명한 문서**
- `/docs/CODING_RULE.md` : 코드 작성 시 지켜야 할 **코딩 스타일, 설계 원칙, 개발 규칙을 정리한 문서**
- `/docs/FUNCTIONS.md` : 프로젝트에서 제공하는 **주요 기능 목록과 기능별 설명을 정리한 문서**
- `/docs/TODOLIST.md` : 현재 진행해야 할 **개발 작업 목록과 진행 상태를 관리하는 문서**

## 7. AI Work Log Rule
- AI 도움으로 변경한 내용이 있으면, 변경 건마다 작업 노트를 1개 작성한다: `/docs/aiwork/WORK_yyyyMMdd_HHmmss.md`
- 작업 노트에는 다음을 포함한다: 목표(goal), 핵심 변경사항(key changes), 수정/추가한 파일(touched files), 실행/테스트한 명령(run/test commands), 후속 할 일(follow-ups)
- `/docs/aiwork/`에 적재하는 작업 노트 본문은 한국어로 작성한다 (명령어/코드/파일명은 원문 그대로 표기)
