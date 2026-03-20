# TODO 리스트

> 마지막 업데이트: 2026-03-20  
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

## 3. 유튜브 댓글 조회/저장
- [x] 유튜브 댓글분석 URL 입력 화면
- [x] 유튜브 API로 댓글 수집 + 다음페이지 조회
- [x] 유튜브 댓글 JSON Lines(.jsonl) 파일 저장
- [x] 저장 이력 정보 DB 저장 (youtube_comment_analysis_histories)

## 4. 유튜브 댓글 분석 (예정)
- [x] 분석 메인 화면 추가: `youtubeCommentAnalysis.html` / `youtubeCommentAnalysisView.html`
- [x] 저장 이력 목록 조회(페이징/필터) + 액션(전처리/상세)
- [x] 댓글 전처리 파이프라인: 정규화 규칙 확정 + 전처리 파일 저장 + DB 업데이트(preprocessed_* 컬럼)
- [x] 댓글 키워드 분석 : Top 10 키워드, 주제별 묶음
- [x] 감정분석
- [x] 네트워크 분석
