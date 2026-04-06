# TODO 리스트

> 마지막 업데이트: 2026-04-06  
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
- [x] 댓글 수집 버튼 모달화(이력/분석 화면에서 URL 입력)
- [x] 동영상명 자동 저장 + 비고(remark) 입력 추가
- [x] URL 입력 후 저장(이력 생성) -> 전처리 -> 분석까지 일괄 수행
- [x] 목록화면 : 감성사전 조회(모달)
- [x] 목록화면 : 커스텀사전 조회/추가/수정/삭제(모달)
- [x] 감정분석 댓글 결과를 item 테이블로 적재(재검토/제안 반영 기반)
- [x] 감정분석 댓글 결과 LLM 재판단 요청 기능 추가
- [x] LLM 결과 기반 커스텀사전 제안 생성 및 반영 기능 추가
- [x] 유튜브 댓글 분석 도움말 MD 제공(팝오버/상세 도움말)
- [ ] 구 수집 화면(/users/youtubeComment/search) 정리: "데이터 가져오기" 제거/리다이렉트 및 메뉴 노출 최소화

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
- [x] ollama 챗봇 메뉴 구성
- [x] ollama 챗봇 대화창 구현

## 7. Amazon Product
- [x] 제품 조회 목록 구현
- [x] 한글 검색 시 LLM을 통해 영문 키워드로 변환 후 ES 조회

### 개선
- [x] LLM 요청 시 번역+키워드 확장(동의어 유사) 결과를 1줄 포맷으로 반환하도록 개선
- [x] 출력 포맷/언어/키워드 품질/도메인 제약을 규칙으로 못 박는 강화 등 LLM 지시어(prompt) 강화
- [x] 동일 검색어의 LLM 결과를 Redis에 캐싱하여 반복 호출 지연/비용 감소
