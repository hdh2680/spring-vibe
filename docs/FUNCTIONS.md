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

## 4. 사용자(Users)

### 4.1 유튜브 댓글분석(Youtube Comment)
- Base(댓글 수집): `/users/youtubeComment/search`
- Base(분석/이력): `/users/youtubeComment/analysis`
- 화면 템플릿: `src/main/resources/templates/html/users/youtubeComment/`
  - 수집: `youtubeCommentSearch.html`
  - 이력/목록: `youtubeCommentAnalysis.html`
  - 상세: `youtubeCommentAnalysisView.html`
- 저장 경로(첨부/내보내기): `app.storage.attachments-dir/youtubeComment/{yyyyMMdd_HHmmss}.jsonl`
- 외부 연동: YouTube Data API (`integrations.youtube.api-key`)

- 관련 테이블
  - `youtube_comment_analysis_histories`: 댓글 수집/분석 이력
    - `video_url`, `original_file_path`, `original_saved_at`
    - `preprocessed_file_path`, `preprocessed_saved_at`
    - `analysis_requested_at` 등 진행 상태 컬럼
  - `youtube_comment_analysis_top_keywords`: 키워드 Top N 결과
  - `youtube_comment_analysis_topic_groups`: 토픽(주제) 그룹 결과
  - `youtube_comment_analysis_sentiments`: 감정분석 결과(분포/샘플 등)
  - `youtube_comment_analysis_word_networks`: 단어 동시출현 네트워크 결과

- 화면/기능
  - 댓글 수집 화면:
    - `GET /users/youtubeComment/search` (폼)
    - `POST /users/youtubeComment/search` (댓글 목록 조회)
    - `POST /users/youtubeComment/search/export` (전체 댓글 JSONL 저장 + 이력 저장)
    - `POST /users/youtubeComment/search/exportAsync` (JSON 응답으로 export 결과 반환)
  - 분석/이력:
    - `GET /users/youtubeComment/analysis` (이력 목록)
    - `POST /users/youtubeComment/analysis/preprocess?id={historyId}` (전처리 수행 및 결과 파일 저장)
    - `GET /users/youtubeComment/analysis/view?id={historyId}` (상세: 전처리 프리뷰/카운트/분석 탭)
    - `POST /users/youtubeComment/analysis/analyze?id={historyId}` (분석 수행 및 결과 적재)
    - `GET /users/youtubeComment/analysis/result.json?id={historyId}` (분석 결과 JSON)

### 4.2 개발 블로그 검색(Dev Search)
- Base(화면): `/users/devSearch`
- Base(API): `/api/dev-search`
- 관련 테이블: `velog_post`
- Elasticsearch 인덱스: `dev-search-posts`
- 토글(local): `DEV_SEARCH_ENABLED=true|false`
  - `dev-search.elasticsearch.enabled`
  - `spring.data.elasticsearch.repositories.enabled`
  - `management.health.elasticsearch.enabled`

- 화면/기능
  - 목록: `GET /users/devSearch/list`
    - 검색어 없으면 DB 최신순 목록
    - 검색어 있으면 ES enabled 시 ES 검색 후 DB hydrate, ES disabled/다운 시 DB LIKE 검색 fallback
    - ES 메타/집계 표출: `total hits`, `took(ms)`, `top tags`, `_score`
    - 검색어 자동완성: datalist UI + `GET /api/dev-search/suggest`
  - 상세: `GET /users/devSearch/view?id={velog_post.id}`
    - 본문(body) Markdown -> safe HTML 변환 렌더링
  - (개발용) ES 재색인: `POST /users/devSearch/reindex`

- API(개발용)
  - ping: `GET /api/dev-search/ping`
  - 검색: `GET /api/dev-search/search?q={keyword}&page={0..}&size={1..50}`
  - 자동완성(제목): `GET /api/dev-search/suggest?q={prefix}&limit={1..20}`
  - 전체 재색인: `POST /api/dev-search/reindex`
  - 문서 전체 삭제: `POST /api/dev-search/clear`

## 3. 패키지 구조
- `{{ROOT_PACKAGE}}.system.config`
- `{{ROOT_PACKAGE}}.system.exception`
- `{{ROOT_PACKAGE}}.system.security`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.controller`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.service`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.repository`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.mapper`
- `{{ROOT_PACKAGE}}.{{DEVELOP_PACKAGE}}.domain`
