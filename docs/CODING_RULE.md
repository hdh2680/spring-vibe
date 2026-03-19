# 프로젝트 코딩 컨벤션 (RE)

본 문서는 프로젝트에서 개발자가 준수해야 할 **코딩 규칙, 네이밍, 주석, 예외 처리, 화면(View) 템플릿 경로** 등을 정의합니다.

## 문서 규칙
- 인코딩: UTF-8
- 기능 요구사항: `/docs/PRD.md`
- 아키텍처/패키지 구조: `/docs/ARCHITECTURE.md`


## 인코딩 규칙(중요)
- **소스/설정/템플릿 파일은 UTF-8 (no BOM)로 통일한다.**
  - BOM(\ufeff)이 섞이면 Java 컴파일 에러(`illegal character: \ufeff`)나 런타임 파싱 문제로 이어질 수 있다.
- PowerShell의 `Set-Content -Encoding utf8`는 환경에 따라 BOM이 포함될 수 있으므로 주의한다.
  - 권장: IntelliJ에서 파일 인코딩을 UTF-8로 저장하고, BOM은 사용하지 않는다.
- 레포에는 UTF-8 BOM 체크 스크립트를 둔다.
  - 실행: `powershell -NoProfile -ExecutionPolicy Bypass -File docs/scripts/check_utf8_bom.ps1 -Root .`
  - Git hook(옵션): `.git/hooks/pre-commit`에서 staged 파일에 BOM이 있으면 커밋을 막는다.

## 0. 변수 정의
- `/README.md` 기준 플레이스홀더 사용

---

## 1. Java 패키지 및 클래스 네이밍

### 1.1 패키지 네이밍
- 소문자 사용, 단어 구분 시 점(.) 사용
- 예시:
  - `{{ROOT_PACKAGE}}.system.config`
  - `{{ROOT_PACKAGE}}.system.exception`
  - `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.controller`
  - `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.service`
  - `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.repository`
  - `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.mapper`
  - `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.domain`

### 1.2 클래스 네이밍
- UpperCamelCase 사용
- 예시:
  - Entity: `SampleEntity`
  - Repository: `SampleRepository`
  - Service: `SampleService`
  - Controller: `SampleController`
  - Mapper: `SampleMapper`

### 1.3 Controller 네이밍(리소스 기준)
- 컨트롤러 클래스명은 기본 매핑 URL의 리소스(복수형)와 동일하게 맞춘다.
- 형식: `{ResourcePlural}Controller`
- 리소스에 다른 접두어를 붙이지 않는다.
  - (예: `/admin/users`인데 `AdminUsersController`처럼 만들지 않음)
- 예시:
  - `/admin/users` -> `UsersController`
  - `/admin/menus` -> `MenusController`
  - `/admin/roleMenus` -> `RoleMenusController`
- 파일명은 클래스명과 동일하게 한다.
  - (예: `UsersController.java`)

---

## 2. 기능별(모듈) 디렉토리 구조 가이드
- Admin 계열 기능은 아래 구조를 기본으로 한다.
  - `src/main/java/springVibe/dev/admin/{feature}/controller`
  - `src/main/java/springVibe/dev/admin/{feature}/service`
  - `src/main/java/springVibe/dev/admin/{feature}/mapper`
  - `src/main/java/springVibe/dev/admin/{feature}/domain`
  - `src/main/java/springVibe/dev/admin/{feature}/dto`
- 일반 사용자(User) 계열 기능은 아래 구조를 기본으로 한다.
  - `src/main/java/springVibe/dev/users/{feature}/controller`
  - `src/main/java/springVibe/dev/users/{feature}/service`
  - `src/main/java/springVibe/dev/users/{feature}/mapper`
  - `src/main/java/springVibe/dev/users/{feature}/domain`
  - `src/main/java/springVibe/dev/users/{feature}/dto`
- 예시:
  - 사용자 관리: `src/main/java/springVibe/dev/admin/users/**`
  - 메뉴 관리: `src/main/java/springVibe/dev/admin/menus/**`
  - (일반 사용자) 프로필: `src/main/java/springVibe/dev/users/profile/**`

---

## 3. 변수 및 메서드 네이밍

### 3.1 변수
- lowerCamelCase 사용
- 의미 있는 이름 사용
- 예시: `userName`, `dataList`, `transactionCount`

### 3.2 메서드
- lowerCamelCase 사용
- 동사형 사용
- 예시: `findAllUsers()`, `validateData()`, `saveTransaction()`

### 3.3 상수
- 모두 대문자 + 언더스코어
- 예시: `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT_MS`

---

## 4. 코드 스타일

### 4.1 들여쓰기
- 스페이스 4칸 사용
- 탭 금지

### 4.2 중괄호
- K&R 스타일 권장
```java
if (condition) {
    doSomething();
} else {
    doSomethingElse();
}
```

### 4.3 줄 길이
- 최대 120자
- 길어지는 경우 적절히 개행

### 4.4 공백
- 연산자, 쉼표, 콜론 주변 공백 사용
- 예시: `int sum = a + b;`

---

## 5. 주석 및 문서화

### 5.1 클래스/인터페이스 주석
- 목적, 주요 기능 위주로 작성
- 불필요한 장문/중복 설명은 지양

### 5.2 메서드 주석
- 입력 파라미터, 반환값, 예외 처리 명시

### 5.3 인라인 주석
- 필요한 경우에만 작성 (코드 이해에 도움되는 경우)

---

## 6. 예외 처리
- 모든 예외는 `{{ROOT_PACKAGE}}.system.exception.BaseException`을 상속
- `GlobalExceptionHandler`를 통해 JSON 표준 포맷으로 반환
- 서비스 레이어에서 처리 가능한 예외는 적절히 잡아서 커스텀 예외로 변환

---

## 7. Logging
- 기본은 Spring Boot 기본 로깅(프로젝트 기본 설정 기준) 사용
- 각 레벨별 규칙:
  - DEBUG: 개발 단계 상세 로그
  - INFO: 주요 비즈니스 이벤트
  - WARN: 예외 발생 가능성, 비정상 상태
  - ERROR: 처리 불가 예외
- 예시: `logger.info("data processed successfully: {}", dataId);`

---

## 8. 기타 규칙
- Lombok 사용 가능: Getter/Setter, Constructor
- 불필요한 주석 제거
- Magic Number 사용 금지 -> 상수로 정의
- Transaction 관리 필수 -> 서비스 레이어에서 `@Transactional` 적용

---

## 9. View Template (Thymeleaf)
- 화면(View) 템플릿은 `src/main/resources/templates/html/**` 하위에 생성한다.
- `layout/app.html` 구조에서 Controller는 `contentTemplate`에 `html/...` prefix를 포함해서 지정한다.
  - 예: `html/admin/users/list`, `html/admin/menus/view`
