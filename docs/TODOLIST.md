# TODO 리스트

> 마지막 업데이트: 2026-03-26  
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

## 4. 유튜브 댓글 분석
- [x] 분석 메인 화면 추가: `youtubeCommentAnalysis.html` / `youtubeCommentAnalysisView.html`
- [x] 저장 이력 목록 조회(페이징/필터) + 액션(전처리/상세)
- [x] 댓글 전처리 파이프라인: 정규화 규칙 확정 + 전처리 파일 저장 + DB 업데이트(preprocessed_* 컬럼)
- [x] 댓글 키워드 분석 : Top 10 키워드, 주제별 묶음
- [x] 감정분석
- [x] 네트워크 분석

## 5. 개발 블로그 검색
- [x] velog 수집기 구현(1회성 적재 도구)
- [x] 도커 기반 ES(+Kibana) 서비스 실행환경 구성
- [x] velog 목록 화면 구현: `/users/devSearch/list`
- [x] velog 상세 화면 구현: `/users/devSearch/view`
- [x] 검색 시 ES 검색 되도록 구현(ES -> id/score/집계 -> DB hydrate + fallback)
- [x] ES 재색인/전체삭제 API 및 화면 버튼 제공
- [ ] (선택) ES 분석기(nori)/user_dictionary 적용 및 검색 품질 고도화
- [ ] (선택) 검색 하이라이트(매칭 스니펫) 표출
- [ ] (선택) DB 삭제/비공개 반영을 위한 ES 동기화 전략(인덱스 클린업/재색인 개선)
- [ ] (선택) reindex 대용량 대응(paging/bulk) 및 운영 안전장치
