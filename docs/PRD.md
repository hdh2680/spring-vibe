# PRD

본 문서는 프로젝트의 목적, 사용자, 핵심 기능 등 제품 요구사항을 정의합니다.

## 문서 규칙
- 인코딩: UTF-8
- 아키텍처/패키지 구조 상세: `/docs/ARCHITECTURE.md`
- 코딩/네이밍/화면 템플릿 경로 규칙: `/docs/CODING_RULE.md`

## 권한(Role)
- `ROLE_ADMIN` : 관리자
- `ROLE_USER` : 사용자

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
유튜브 URL을 입력받아 API를 통해 댓글 목록을 수집, 해당 데이터를 전처리, 분석 과정을 거쳐 시각화 자료를 제공한다.
Client -> (URL입력) -> `Contoller` -> `Service` -> `Youtube Data API` -> (예정)데이터 분석 -> Response API

- 패키지 : `src/main/java/springVibe/dev/users/youtubeComment`
- 매핑
  - 댓글 수집: `/users/youtubeComment/search`
  - 분석/이력: `/users/youtubeComment/analysis`
- 화면: `templates/html/users/youtubeComment/`
  상단에 유튜브 URL 입력하는 INPUT박스가 존재하고 데이터가져오기 버튼을 클릭하면 api호출하여 하단에 댓글목록을 제공한다.
- 관련 테이블
  - `youtube_comment_analysis_histories` : 저장 버튼 클릭 시 영상 처리/분석 이력

#### 4.1 유튜브 댓글 조회
- 컨트롤러 : `YoutubeCommentSearchController`
- 화면 : `youtubeCommentSearch.html`
- 기능
  - `templates/html/users/youtubeComment/youtubeCommentSearch.html`에서 URL을 입력받는다.
  - 입력받은 URL로 youtube API를 사용해 댓글 데이터를 수집한다.
  - 댓글 내용을 목록으로 조회한다.
  - 저장 버튼 클릭 시 `application.yml`의 `app.storage.attachments-dir/youtubeComment/{yyyyMMdd_HHmmss}.jsonl` 형식으로 댓글 데이터를 전부 수집하여 JSON Lines (.jsonl) 파일을 저장하고 저장한 정보를 `youtube_comment_analysis_histories` 테이블에 저장한다.
    - `video_url` : 입력받은 URL
    - `original_file_path` : 저장 버튼 클릭 시 저장된 파일경로
    - `original_saved_at` : 저장 버튼 클릭 일시

#### 4.2 유튜브 댓글 분석
- 컨트롤러 : `YoutubeCommentAnalysisController`
- 화면 : `youtubeCommentAnalysis.html`
- 기능 
  - `youtube_comment_analysis_histories` 테이블 목록을 조회한다. 목록 actions은 `preprocessed_saved_at` 컬럼이 null이라면 전처리수행, 전처리 수행이 됐다면 view(상세)로 이동할 수 있다.
  - 테이블 목록에서 전처리 클릭 시 원본 `original_file_path`의 파일을 읽어 댓글 전처리 수행 후 원본형태에 전처리 된 문구열을 추가하여 JSON Lines(.jsonl) 파일을 저장하고 `preprocessed_file_path`, `preprocessed_saved_at` 컬럼을 수정한다.
  - 전처리 정규화
    - 기본 정규화: 유니코드 정규화(NFKC), 개행/탭 -> 공백, 연속 공백 축약, trim
    - 노이즈 제거: URL 제거, HTML 엔티티/태그 제거, 불필요한 특수문자 제거(단, `! ? ㅋㅋ ㅠㅠ` 등 감정 신호는 완전 삭제보다 축약/토큰화 권장)
    - 반복 문자 축약: `ㅋㅋㅋㅋㅋㅋ` -> `ㅋㅋ`, `!!!!` -> `!` 등 과도한 반복을 의미 보존 형태로 축약
    - 멘션/해시태그 처리: `@id`, `#tag`는 제거 또는 토큰화(@MENTION, #TAG) 중 택1
    - 스팸/광고/중복 처리(옵션): 광고/유도 문구 룰 기반 필터, 완전 동일 댓글 중복 제거, 언어 감지로 대상 외 언어 분리
  - `youtubeCommentAnalysisView.html` 분석 상세화면이며 탭으로 화면을 구성함. 전처리, 댓글분석 탭들이 존재하며 전처리 탭에서 `analysis_requested_at`이 컬럼의 NULL여부를 판단해 분석수행 버튼을 활성화, 해당버튼으로 댓글 분석을 요청한다.
  - 데이터 분석
    - 댓글 키워드 분석
      - Top N 키워드 : `youtube_comment_analysis_top_keywords`에 분석결과를 적재한다.
      - 주제별 묶음 : `youtube_comment_analysis_topic_groups`에 분석결과를 적재한다.
    - (예시)전처리 된 댓글 내용을 AI API(GPT)에 댓글 감정분석을 의뢰한다.
    - (예시)댓글 목록, 총 댓글 갯수 대비 감정분석(긍정, 중립, 부정) 분포율, 키워드 분석 순위를 시각화하여 `templates/html/users/youtubeComment/youtubeCommentMain.html` 페이지 하단에 제공한다.
    - (예시)시간흐름분석 초반에서 후반부로 갈수록 감정변화가 어떻게 변하는지
    - (예시)댓글요약(AI활용) 많은댓글을 3줄로 요약
  

## 결정 사항(Decision Log)
- 화면 템플릿 경로 표준화: `templates/html/**` 하위로 통일 (구 `templates/admin/**` 사용 금지)
- Controller 네이밍: 기본 매핑 URL 리소스(복수형) 기준 `{ResourcePlural}Controller` 사용 (예: `/admin/users` -> `UsersController`)

