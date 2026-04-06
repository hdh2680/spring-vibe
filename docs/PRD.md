# PRD

본 문서는 프로젝트의 목적, 사용자, 핵심 기능 등 제품 요구사항을 정의합니다.

## 문서 규칙
- 인코딩: UTF-8
- 아키텍처/패키지 구조 상세: `/docs/ARCHITECTURE.md`
- 코딩/네이밍/화면 템플릿 경로 규칙: `/docs/CODING_RULE.md`

## 권한(Role)
- `ROLE_ADMIN` : 관리자
- `ROLE_USER` : 사용자

## 챗봇(ollama)

- 목적: 로컬 Ollama 기반의 간단한 사내 도우미(요약/번역/개발 Q&A). 인터넷 뉴스 “근황” 조회 같은 실시간 정보는 제공하지 않는다.
- UI: 좌측 사이드바의 `AI Chat` 버튼 클릭 시 모달(폰 메신저 스타일)로 대화창 오픈.
  - 레이아웃: `src/main/resources/templates/layout/app.html`
  - 스크립트/스타일: `src/main/resources/static/js/chat.js`, `src/main/resources/static/css/chat.css`
- API:
  - `POST /api/chat` 요청 `{ "message": "..." }` -> 응답 `{ "reply": "..." }`
  - `DELETE /api/chat` 대화 히스토리 초기화(세션 기반)
  - `GET /api/chat/health` Ollama 연결 상태 확인
- 설정:
  - `ollama.base-url` (env: `OLLAMA_BASE_URL`, 기본값 `http://localhost:11434`)
  - `ollama.model` (env: `OLLAMA_MODEL`, 기본값 `qwen2.5:1.5b`)

## 공통
### 로그인
- 패키지: `src/main/java/springVibe/dev/common/`
- 매핑: `/login`
- 화면: `templates/html/login.html`

### 로그인 로그
- 패키지: `src/main/java/springVibe/dev/common/`
- 관련 테이블
  - `login_logs` : 사용자 로그인 이력

## 메뉴(관리 기능)
> 화면(View) 템플릿의 기본 위치는 `src/main/resources/templates/html/**` 하위이며, Controller의 `contentTemplate` 지정 규칙은 `/docs/CODING_RULE.md`를 따른다.

### 1. 사용자 관리
- 패키지: `src/main/java/springVibe/dev/admin/users/`
- 매핑: `/admin/users`
- 화면: `templates/html/admin/users/`
- 관련 테이블
  - `users` : 사용자
  - `roles` : 권한
  - `user_roles` : 사용자-권한 매핑
- 기능
  - 사용자 목록 조회: `/list`
  - 사용자 상세 조회: `/view`
  - 사용자 등록: `/regist`
  - 사용자 수정: `/regist`
    - 사용자 상태 컬럼 `status` 수정
    - 권한 부여/회수

### 2. 메뉴 관리
- 패키지: `src/main/java/springVibe/dev/admin/menus/`
- 매핑: `/admin/menus`
- 화면: `templates/html/admin/menus/`
- 관련 테이블
  - `menus` : 메뉴
- 기능
  - 메뉴 목록 조회: `/list`
  - 메뉴 상세 조회: `/view`
  - 메뉴 등록: `/regist`
  - 메뉴 수정: `/regist`
  - 메뉴 삭제

### 3. 권한-메뉴 관리
- 패키지: `src/main/java/springVibe/dev/admin/roleMenus/`
- 매핑: `/admin/roleMenus`
- 화면: `templates/html/admin/roleMenus/`
- 관련 테이블
  - `menus` : 메뉴
  - `roles` : 권한
  - `role_menus` : 권한별 메뉴
- 기능
  - 권한-메뉴 관리: `/manage`
    - 좌측 권한 목록 조회
    - 우측 메뉴 목록 조회
    - 체크박스로 메뉴 권한 부여/회수

### 4. 유튜브 댓글분석
유튜브 댓글 분석 도메인의 상세 요구사항/흐름은 아래 문서를 참조한다.

- `/docs/PRD-youtubeComment.md`

### 5. Elastic Search

#### 5.0 Elasticsearch 터미널 구동 (Dev)
로컬 개발용 Elasticsearch는 `docker-compose.dev-search.yml`로 구동한다.

사전조건:
- Docker Desktop 실행

Elasticsearch만 실행:
```powershell
cd C:\workspace\java_lec\spring-vibe
docker compose -f docker-compose.dev-search.yml up -d elasticsearch
```

Elasticsearch + Kibana 실행:
```powershell
cd C:\workspace\java_lec\spring-vibe
docker compose -f docker-compose.dev-search.yml up -d
```

중지:
```powershell
cd C:\workspace\java_lec\spring-vibe
docker compose -f docker-compose.dev-search.yml down
```

데이터 초기화(볼륨 삭제):
```powershell
cd C:\workspace\java_lec\spring-vibe
docker compose -f docker-compose.dev-search.yml down -v
```

동작 확인:
```powershell
curl http://localhost:9200
```

주의:
- `docker-compose.dev-search.yml`에서 `ES_JAVA_OPTS`로 힙을 잡는다. 메모리 부족하면 낮게 유지(예: `-Xms256m -Xmx256m`).
- Docker Desktop에서 Images -> Run 으로 직접 띄울 경우, 반드시 포트 `9200:9200`를 publish 해야 `http://localhost:9200` 접근이 된다.

#### 5.1 개발 블로그(velog)
개발 블로그 검색 도메인의 상세 요구사항/흐름은 아래 문서를 참조한다.
- `/docs/PRD-devSearch.md`

#### 5.2 Amazon Product
Amazon Product 도메인의 상세 요구사항/흐름은 아래 문서를 참조한다.
- `/docs/PRD-amazonProduct.md`

#### Redis (Local Run)
Amazon Product 검색에서 한글 검색어를 LLM(Ollama)로 번역/확장할 때, 동일 검색어의 LLM 결과를 Redis에 캐싱한다.

Run:
```powershell
docker compose -f docker-compose.redis.yml up -d
```

Stop:
```powershell
docker compose -f docker-compose.redis.yml down
```

App config:
- default: `localhost:6379`
- env: `REDIS_HOST`, `REDIS_PORT`


## 결정 사항(Decision Log)
- 화면 템플릿 경로 표준화: `templates/html/**` 하위로 통일 (구 `templates/admin/**` 사용 금지)
- Controller 네이밍: 기본 매핑 URL 리소스(복수형) 기준 `{ResourcePlural}Controller` 사용 (예: `/admin/users` -> `UsersController`)

