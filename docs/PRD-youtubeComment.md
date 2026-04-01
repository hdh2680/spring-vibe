# 유튜브 댓글분석
유튜브 URL을 입력받아 API를 통해 댓글 목록을 수집, 해당 데이터를 전처리, 분석 과정을 거쳐 시각화 자료를 제공한다.
Client -> (URL입력) -> `Contoller` -> `Service` -> `Youtube Data API` -> 데이터 분석 -> Response API

- 패키지 : `src/main/java/springVibe/dev/users/youtubeComment`
- 매핑
  - 댓글 수집: `/users/youtubeComment/search`
  - 분석/이력: `/users/youtubeComment/analysis`
- 화면: `templates/html/users/youtubeComment/`

## 1. 유튜브 댓글 조회
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

## 2. 유튜브 댓글 분석
- 컨트롤러 : `YoutubeCommentAnalysisController`
- 화면 : `youtubeCommentAnalysis.html`
- 기능 
  - `youtube_comment_analysis_histories` 테이블 목록을 조회한다. 목록에서 영상명을 클릭하면 상세(view)로 이동할 수 있다.
  - 저장(댓글 수집) 시 전처리/분석까지 일괄 수행한다.
  - 전처리 정규화
    - 기본 정규화: 유니코드 정규화(NFKC), 개행/탭 -> 공백, 연속 공백 축약, trim
    - 노이즈 제거: URL 제거, HTML 엔티티/태그 제거, 불필요한 특수문자 제거(단, `! ? ㅋㅋ ㅠㅠ` 등 감정 신호는 완전 삭제보다 축약/토큰화 권장)
    - 반복 문자 축약: `ㅋㅋㅋㅋㅋㅋ` -> `ㅋㅋ`, `!!!!` -> `!` 등 과도한 반복을 의미 보존 형태로 축약
    - 멘션/해시태그 처리: `@id`, `#tag`는 제거 또는 토큰화(@MENTION, #TAG) 중 택1
    - 스팸/광고/중복 처리(옵션): 광고/유도 문구 룰 기반 필터, 완전 동일 댓글 중복 제거, 언어 감지로 대상 외 언어 분리
- `youtubeCommentAnalysisView.html` 분석 상세화면이며 탭으로 화면을 구성함. 전처리, 댓글분석 탭들이 존재하며, 상세 화면의 분석수행 버튼은 전처리 -> 분석을 한 번에 수행한다.
  - 데이터 분석
    - 키워드 분석
      - Top N 키워드 : `youtube_comment_analysis_top_keywords`에 분석결과를 적재한다.
      - 주제별 묶음 : `youtube_comment_analysis_topic_groups`에 분석결과를 적재한다.
    - 감정분석 : `youtube_comment_analysis_sentiments`에 분석결과를 적재한다.
      - 사전 기반 감정분석으로 댓글을 점수화 하여 긍정/중립/부정의 분포율을 제공한다.
        - 사전 출처 : https://github.com/park1200656/KnuSentiLex (KNU 한국어 감성사전)
          - 부정 : neg_pol_word.txt
          - 중립 or Unknown : obj_unknown_pol_word.txt
          - 긍정 : pos_pol_word.txt
          - 점수 : SentiWord_Dict.txt
        - txt파일을 시스템에서 읽기 쉽게 tsv 하나파일로 만듦. : `src/main/resources/static/docs/youtubeComment/sentiment_lexicon.tsv`
      - 시간의 흐름에 따라 감정변화가 어떻게 변하는지 긍정/중립/부정을 그래프로 제공한다. (그래프단위 : 시간(3h), y축 : 비율(%))
      - 감정분포별 댓글을 최대 50개까지 저장하여 하단 목록으로 제공한다.
    - 네트워크 분석 (Word Co-occurrence Network) : 어떤 단어들이 동시에 자주 등장하는지 연결망을 만드는 분석입니다.
      - `youtube_comment_analysis_word_networks`에 분석결과를 적재한다.


# 패치노트

- 유튜브 댓글분석 메뉴에서 댓글수집 버튼 클릭시 모달팝업화면으로 수집화면 호출 : 기존 수집화면은 삭제
- 유튜브 댓글 저장시 전처리, 분석 일괄 수행
- API로 영상명 저장, 사용자 비고 저장
- 감성사전, 커스텀사전 팝업조회 기능 추가 및 커스텀사전 추가/삭제 기능 추가
- 기존에 감성분석 댓글 결과를 50개 JSON에 포함시켜 저장하던것을 테이블적재로 변경
  - 사전으로 분석하다보니 분석의 오류여지가 많고 중립된 데이터가 많음.
  - ollama LLM으로 한번더 검증결과를 진행하여 custom사전 확장 및 데이터 분석 재진행