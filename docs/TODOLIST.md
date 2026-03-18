# TODO 리스트

> 마지막 업데이트: 2026-03-18  
> 기준: PRD + 현재 코드/템플릿/DB 스키마 현황

## 1. 문서/초기화
- [x] README.md 작성
- [x] /docs/ARCHITECTURE.md 작성
- [x] /docs/CODING_RULE.md 작성
- [x] /docs/PRD.md 작성
- [x] /docs/DATABASE_SCHEMA.md 작성
- [x] /docs/FUNCTIONS.md 작성
- [x] 프로젝트 구조 생성 및 샘플 정리

## 2. 인증/관리자
- [x] 사용자 로그인/권한
- [x] 로그인 이력 적재 (login_logs)
- [x] 관리자메뉴-사용자관리
- [x] 관리자메뉴-메뉴관리
- [x] 관리자메뉴-권한메뉴관리

## 3. 유튜브 댓글 조회/저장 (MVP)
- [x] 유튜브 댓글분석 URL 입력 화면
- [x] 유튜브 API로 댓글 수집 + 다음페이지 조회
- [x] 유튜브 댓글 JSON Lines(.jsonl) 파일 저장
- [x] 저장 이력 정보 DB 저장 (youtube_comment_analysis_histories)

## 4. 유튜브 댓글 분석 (예정)
- [ ] 분석 메인 화면 추가: `youtubeCommentAnalysis.html` / `youtubeCommentMain.html`
- [ ] 저장 이력 목록 조회(페이징/필터) + 액션(전처리/상세)
- [ ] 댓글 전처리 파이프라인: 정규화 규칙 확정 + 전처리 파일 저장 + DB 업데이트(preprocessed_* 컬럼)
- [ ] 감정분석: 모델/프롬프트/비용/캐싱 전략 결정 후 구현(analysis*_ 컬럼 활용)
- [ ] 키워드(명사) 추출: 라이브러리/방식 선정 후 구현
- [ ] 시각화: 감정 분포, 키워드 Top N, 시간 흐름(댓글 publishedAt 기반)
- [ ] 대량 댓글 처리: API quota/429/403 대응, 재시도 정책, 진행률/비동기 처리(옵션)

## 5. 운영/보안/품질
- [ ] secrets 정리: `integrations.youtube.api-key`는 env var(`YOUTUBE_API_KEY`)로만 주입, 저장소에 실키 커밋 금지
- [ ] 스토리지 디렉토리 생성/권한 체크 및 오류 메시지 개선(app.storage.attachments-dir)
- [ ] 기본 스모크 테스트 시나리오 정리: 로그인, 관리자 메뉴 3종, 유튜브 수집/저장
- [ ] 로그 레벨/민감정보 마스킹 점검(DEBUG 기본값 조정)

## 6. 확장(Backlog)
- [ ] 데이터 품질 관리(테이블 구조/통계/이상치/리포트)
- [ ] 시스템 관리(스케줄링/알람 등)
