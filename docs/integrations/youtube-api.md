# YouTube API (YouTube Data API v3) 연동 가이드

## 1. 목적/범위
- YouTube Data API v3를 사용해 YouTube 리소스(예: 채널/영상/플레이리스트/검색 결과)를 조회한다.
- 본 문서는 "프로젝트에서 YouTube API를 안전하게 설정하고 운영하는 방법"을 정리한다.

## 2. 인증 방식 선택
YouTube Data API는 보통 아래 2가지 중 하나로 접근한다.

### 2.1 API Key (서버 단순 조회용)
- 공개 데이터 조회(검색, 영상/채널 메타데이터 조회 등) 위주면 API Key로 시작한다.
- 사용자 계정 권한이 필요한 작업(내 구독 목록, 내 재생목록 수정 등)은 불가.

필요 값(권장 ENV):
- `YOUTUBE_API_KEY`

### 2.2 OAuth 2.0 (사용자 권한 필요)
- 사용자별 접근(예: 내 채널 정보, 구독/재생목록 변경, 댓글 작성 등)이 필요하면 OAuth 2.0을 사용한다.
- 운영 시 토큰 저장/갱신/철회와 보안이 핵심이다.

필요 값(권장 ENV):
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `GOOGLE_REDIRECT_URI`

## 3. 설정 위치 규칙
- 민감정보(API Key, Client Secret)는 Git에 커밋하지 않는다.
- `application.yml`에는 "ENV 참조"만 둔다.

예시(`src/main/resources/application.yml`):
```yml
integrations:
  youtube:
    api-key: ${YOUTUBE_API_KEY:}
    oauth:
      client-id: ${GOOGLE_CLIENT_ID:}
      client-secret: ${GOOGLE_CLIENT_SECRET:}
      redirect-uri: ${GOOGLE_REDIRECT_URI:}
```

## 4. 쿼터/레이트 리밋
- YouTube Data API는 "쿼터 단위"로 비용이 계산된다(메서드별로 단위가 다름).
- 동일 요청 반복을 피하기 위해 캐시(예: 영상/채널 메타데이터 TTL 캐시)를 적용한다.
- 실패 시 재시도는 "지수 백오프"를 기본으로 하되, 4xx(권한/파라미터 오류)는 즉시 실패 처리한다.

## 5. 에러 처리 가이드
- `401/403`: API Key/OAuth 권한/쿼터/프로젝트 설정 문제 가능성이 큼. 응답 본문의 에러 사유를 로그에 남긴다(비밀값은 마스킹).
- `400`: 파라미터 검증 문제. 서버에서 입력값 검증을 강화한다.
- `429`: 과도한 호출. 캐시/백오프/호출량 제한(서킷 브레이커 포함)을 적용한다.
- `5xx`: 일시 장애일 가능성. 제한된 횟수로 재시도한다.

## 6. 프로젝트 내 책임 분리(권장)
- `integrations/youtube`: YouTube API 클라이언트/DTO/에러 매핑
- `service`: 비즈니스 로직(조회 정책, 캐시 정책, 저장 정책)
- `controller`: API 요청/응답 스키마, 입력 검증

## 7. 테스트 전략(권장)
- 단위 테스트: YouTube API 클라이언트를 인터페이스로 분리하고 Mock으로 검증한다.
- 통합 테스트: 호출 비용과 쿼터 영향을 고려해 로컬/CI에서 기본적으로는 비활성화하고, 수동/스케줄 형태로 실행한다.

## 8. TODO (연동 구현 시 채울 것)
- [ ] 사용 API 목록(예: `search.list`, `videos.list`, `channels.list`)과 요청/응답 샘플
- [ ] 도메인 요구사항에 맞는 캐시 키/TTL 정책
- [ ] 로깅/모니터링(실패율, 응답시간, 쿼터 사용량)
- [ ] OAuth 토큰 저장소/암호화/폐기 정책

## 9. 유튜브 댓글분석 (MVP) - 수집/조회

### 9.1 화면/매핑/패키지
- 매핑: `/users/youtubeComment`
- 화면: `src/main/resources/templates/html/users/youtubeComment/youtubeCommentSearch.html`
- 패키지: `src/main/java/springVibe/dev/users/youtubeComment/**`

### 9.2 실행 전 준비
- `src/main/resources/application.yml`에 `integrations.youtube.api-key` 설정
  - 로컬 MVP 단계에서는 하드코딩도 허용(커밋 금지)

### 9.3 메뉴 등록(사이드바 노출)
본 프로젝트의 좌측 메뉴는 DB의 `menus`/`role_menus` 기반으로 렌더링된다.

1) 관리자 메뉴에서 메뉴 등록
- 이동: `/admin/menus/regist`
- 예시 값
  - `menuKey`: `users_youtube_comment`
  - `menuName`: `유튜브 댓글분석`
  - `path`: `/users/youtubeComment`
  - `sortOrder`: 적절히(예: 50)
  - `isEnabled`: true

2) 권한에 메뉴 부여
- 이동: `/admin/roleMenus/manage`
- `ROLE_USER`(또는 사용할 권한) 선택 후 방금 만든 메뉴 체크
