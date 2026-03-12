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

**Git 형상관리 규칙**은 `/docs/GIT_GUIDELINE.md` 참고  
**코딩 규칙**은 `/docs/CODING_CONVENTION.md` 참고

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
http://localhost:8080/html/sample

## 6. 참고 문서
- 아키텍처 및 패키지 구조: `/docs/ARCHITECTURE.md`
- 코딩 규칙: `/docs/CODING_CONVENTION.md`
- Git 형상관리 규칙: `/docs/GIT_GUIDELINE.md`
- 기능/사용자 안내: 추후 `/docs/FUNCTIONS.md`, `/docs/USER_GUIDE.md` 작성 예정