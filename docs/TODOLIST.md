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

### 개선
- [ ] 유튜브 댓글 조회 기능, 유튜브 댓글 분석 댓글수집 버튼클릭 모달화면으로 이동, 조회 메뉴 삭제
- [ ] 동영상명, 비고 입력 추가
- [ ] 데이터 가져오기 기능 삭제, URL입력 후 바로 분석 수행
- [ ] 댓글 저장 -> 분석까지 일괄진행되도록 기능 변경

## 4. 유튜브 댓글 분석
- [x] 분석 메인 화면 추가: `youtubeCommentAnalysis.html` / `youtubeCommentAnalysisView.html`
- [x] 저장 이력 목록 조회(페이징/필터) + 액션(전처리/상세)
- [x] 댓글 전처리 파이프라인: 정규화 규칙 확정 + 전처리 파일 저장 + DB 업데이트(preprocessed_* 컬럼)
- [x] 댓글 키워드 분석 : Top 10 키워드, 주제별 묶음
- [x] 감정분석
- [x] 네트워크 분석

## 5. 개발 블로그 검색(velog)
- [x] velog 수집기 구현(1회성 적재 도구)
- [x] 도커 기반 ES(+Kibana) 서비스 실행환경 구성
- [x] velog 목록 화면 구현: `/users/devSearch/list`
- [x] velog 상세 화면 구현: `/users/devSearch/view`
- [x] 검색 시 ES 검색 되도록 구현(ES -> id/score/집계 -> DB hydrate + fallback)
- [x] ES 재색인/전체삭제 API 및 화면 버튼 제공

## 6. 챗봇만들기
- [ ] ollama 챗봇 메뉴 구성
- [ ] ollama 챗봇 대화창 구현

## 7. Amazon Product
- [ ] 제품 조회 목록 구현
- [ ] 한글 검색 시 LLM을 통해 영문으로 번역 후 ES 조회