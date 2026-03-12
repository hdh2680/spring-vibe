#. 개요
본 문서는 {{ROOT_PACKAGE}} 프로젝트의 시스템 아키텍처와 패키지 구조, 기술적 의사결정을 정의합니다. 

## 0. 변수 정의
- `/README.md` 기준 플레이스홀더 사용

---

## 1. 프로젝트 백엔드 구성
- **Spring Security**: 인증/인가
- **Transaction 관리**
- **Validation**
- **Global Exception Handling**
- **Actuator**: 모니터링
- **OpenAPI**: Swagger UI
- **Logging**: log4j-api-2.x.jar

---

## 2. DB 설정 안내
- DB: MySQL
- JDBC URL 예시: `jdbc:mysql://localhost:3306/dbname`
- ORM: JPA + MyBatis

---

## 3. 디렉토리 구조 및 레이어 역할
src/main/java/{{ROOT_PACKAGE}}/
├── system/ # 시스템 공통 설정
│ ├── config/ # Security, Transaction, Web, OpenAPI 등 환경 설정
│ │ ├─ SecurityConfig.java
│ │ ├─ TransactionConfig.java
│ │ ├─ WebMvcConfig.java
│ │ └─ OpenApiConfig.java
│ └── exception/ # Global Exception Handler 및 Error Response
│ ├─ BaseException.java
│ └─ GlobalExceptionHandler.java
└── {{DEVELOP_PACKAGE}}/ # 핵심 비즈니스 도메인
├── controller/ # API 엔드포인트 및 요청/응답 관리
├── service/ # 비즈니스 로직 및 트랜잭션 단위
├── repository/ # JPA Repository (Spring data JPA)
├── mapper/ # MyBatis 매퍼 인터페이스 및 XML
└── domain/ # Entity, DTO, VO 객체

---

## 4. 레이어별 설계 및 정책

### 4.1 데이터 계층 설계 (JPA vs MyBatis)
- **Spring data JPA**: 단순 CRUD 작업 및 엔티티 상태 관리
- **MyBatis**: 복잡한 조인, 통계 쿼리, 대량 데이터 정합성 검증 등 최적화 필요 시
- Mapper와 Repository 혼용 시 서비스에서 적절한 인터페이스 주입

### 4.2 보안 및 권한 체계
- 인증: Spring Security
- 세밀한 API 권한: `@PreAuthorize`
- SecurityConfig에 필터/진입점 규칙 정의

### 4.3 예외 처리 정책
- `BaseException` 상속
- `GlobalExceptionHandler` → 표준화된 `ErrorResponse` JSON 반환

### 4.4 정적 자원 관리 정책
- **static/**: CSS, JS, Images, Fonts → `src/main/resources/static/`
- **templates/**: Thymeleaf 템플릿 → 도메인별 폴더로 분리 → `src/main/resources/templates/`

---

## 5. 스켈레톤 클래스 예제

### 5.1 Entity
package {{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.domain;

import jakarta.persistence.*;

@Entity
public class SampleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

### 5.2 Entity
package {{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.repository;

import {{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.domain.SampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleRepository extends JpaRepository<SampleEntity, Long> {}

### 5.3 Service
package {{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.service;

import {{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.domain.SampleEntity;
import {{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.repository.SampleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SampleService {
    private final SampleRepository repository;

    public SampleService(SampleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SampleEntity save(SampleEntity entity) {
        return repository.save(entity);
    }

    public List<SampleEntity> findAll() {
        return repository.findAll();
    }
}

### 5.4 Controller
package {{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.controller;

import {{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.domain.SampleEntity;
import {{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.service.SampleService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sample")
public class SampleController {
    private final SampleService service;

    public SampleController(SampleService service) {
        this.service = service;
    }

    @GetMapping
    public List<SampleEntity> getAll() {
        return service.findAll();
    }

    @PostMapping
    public SampleEntity create(@RequestBody SampleEntity entity) {
        return service.save(entity);
    }
}

### 5.5 Mybatis Mapper XML
<!-- /src/main/resources/mapper/SampleMapper.xml -->
<mapper namespace="{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.mapper.SampleMapper">
    <select id="selectAll" resultType="{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.domain.SampleEntity">
        SELECT id, name FROM sample_entity
    </select>
</mapper>

### 5.6 Global Exception Handler
package {{ROOT_PACKAGE}}.system.exception;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        ErrorResponse response = new ErrorResponse("ERR001", ex.getMessage());
        return ResponseEntity.status(500).body(response);
    }
}

class ErrorResponse {
    private String code;
    private String message;

    public ErrorResponse(String code, String message) {
        this.code = code; this.message = message;
    }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}

### 5.7 Thymeleaf 샘플
<!-- /src/main/resources/templates/html/sample.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Sample Page</title>
</head>
<body>
<h1>Sample Thymeleaf Page</h1>
<p th:text="'Hello, ' + ${name} + '!'"></p>
</body>
</html>
