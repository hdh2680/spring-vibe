# spring-vibe

<div align="center">
  <h3>Spring 기반 Vibe Coding 프로젝트</h3>
  <p>아이디어를 빠르게 프로토타이핑하고, 실무형 기술 스택으로 확장 가능한 구조를 만드는 것을 목표로 합니다.</p>

  <p>
    <img alt="Java" src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white" />
    <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white" />
    <img alt="Maven" src="https://img.shields.io/badge/Maven-Wrapper-C71A36?logo=apachemaven&logoColor=white" />
    <img alt="MySQL" src="https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white" />
    <img alt="Swagger" src="https://img.shields.io/badge/OpenAPI-Swagger%20UI-85EA2D?logo=swagger&logoColor=black" />
  </p>

  <p>
    <a href="https://github.com/hdh2680/spring-vibe">Repository</a>
    ·
    <a href="#quick-start">Quick Start</a>
    ·
    <a href="#docs">Docs</a>
    ·
    <a href="#screenshots">Screenshots</a>
  </p>
</div>

---

## 📌 TL;DR

- 로컬 실행: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`

---

## ✨ What’s Inside

- 🧩 Admin UI: 메뉴/권한/사용자 관리(Thymeleaf)
- 🔐 Spring Security 기반 인증/인가
- 🗃️ JPA + MyBatis 혼합(단순 CRUD는 JPA, 복잡 쿼리는 MyBatis)
- 📊 (예시) YouTube 댓글 분석 도메인 및 문서화

---

<a id="quick-start"></a>

## 🚀 Quick Start

### 1) Prerequisites

- Java 17
- MySQL 8+

### 2) Configure

`src/main/resources/application.yml`에서 DB 접속 정보를 설정합니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/springVibe?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul
    username: root
    password: root
```

외부 연동 키 같은 민감정보는 커밋하지 말고 환경변수로 주입하세요.

- 예시: `YOUTUBE_API_KEY`, `APP_STORAGE_ATTACHMENTS_DIR`

### 3) Run

```bash
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

### 4) Open

- Main: `http://localhost:8080/`
- Login: `http://localhost:8080/login`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Actuator Health: `http://localhost:8080/actuator/health`

---

## 🧱 Tech Stack

- Backend: Java 17, Spring Boot 3.2.x
- View: Thymeleaf
- Database: MySQL 8+
- ORM/SQL: Spring Data JPA, MyBatis
- Docs: OpenAPI 3(Swagger UI), Spring Actuator
- Build: Maven Wrapper

---

## 🗂️ Project Structure (High Level)

도메인(기능) 단위로 패키지를 구성합니다.

- `springVibe/dev/common`: 공통(로그인, 메뉴 등)
- `springVibe/dev/admin/*`: 어드민 기능(유저/권한/메뉴 등)
- `src/main/resources/mapper/**`: MyBatis XML
- `src/main/resources/templates/**`: Thymeleaf 템플릿

자세한 구조/원칙은 아키텍처 문서를 참고하세요: `docs/ARCHITECTURE.md`

---

<a id="screenshots"></a>

## 🖼️ Screenshots

스크린샷을 `docs/images/`에 넣고 README에 연결하는 방식이 가장 편합니다.

```md
![메인 화면](docs/images/main.png)
![관리자 - 사용자 목록](docs/images/admin-users.png)
```

HTML로 크기를 고정하고 싶다면:

```html
<img src="docs/images/main.png" width="900" />
```

---

<a id="docs"></a>

## 📚 Docs

기획/구조/규칙/기능은 아래 문서에 정리되어 있습니다.

- [docs/PRD.md](docs/PRD.md): 요구사항(목적, 사용자, 핵심 기능)
- [docs/DATABASE_SCHEMA.md](docs/DATABASE_SCHEMA.md): DB 스키마
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): 아키텍처/패키지 설계
- [docs/CODING_RULE.md](docs/CODING_RULE.md): 코딩 규칙
- [docs/FUNCTIONS.md](docs/FUNCTIONS.md): 구현 기능 목록
- [docs/TODOLIST.md](docs/TODOLIST.md): 작업 목록/진행 상태
- [docs/integrations/youtube-api.md](docs/integrations/youtube-api.md): YouTube API 가이드

---

## 🧪 Test

```bash
./mvnw test
```

Windows:

```bash
mvnw.cmd test
```

---

## 🧾 Changelog

- [CHANGELOG.md](CHANGELOG.md)

---

## 🤝 Contributing (Optional)

`docs/CODING_RULE.md`와 `docs/ARCHITECTURE.md`를 우선 확인한 뒤 작업합니다.

---

## 📄 License (Optional)

라이선스를 정했다면 여기에 표기하세요. 예: `MIT`
