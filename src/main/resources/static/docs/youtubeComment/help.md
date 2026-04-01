# 유튜브 댓글 분석 도움말

이 기능은 유튜브 URL로 댓글을 수집한 뒤, 전처리된 텍스트를 기반으로 키워드/주제/감정/네트워크 분석을 수행합니다.  
감정분석은 사전 기반으로 1차 분류하고, (향후) LLM(Ollama)로 오분류를 재판단해 보정하는 흐름을 목표로 합니다.

## 전체 흐름

1. 댓글 수집(YouTube Data API)
2. 원본 저장(JSONL) + 이력 저장(DB: youtube_comment_analysis_histories)
3. 전처리 수행(원본 JSONL -> 전처리 JSONL)
4. 분석 수행
5. 결과 저장(DB)
6. 화면에서 결과 조회

## 1) 댓글 수집

- 입력: 유튜브 영상 URL
- 방식: YouTube Data API로 댓글 목록을 조회
- 저장:
  - 원본 댓글 파일을 JSON Lines(.jsonl)로 저장
  - 경로 예: `attachments/youtubeComment/{yyyyMMdd_HHmmss}.jsonl`
  - 저장한 파일 경로와 URL/영상명 등을 이력 테이블에 기록

## 2) 전처리(정규화)

목표는 분석에 방해되는 노이즈를 줄이되, 감정 신호(예: ㅋㅋ, ㅠㅠ, !, ?)는 최대한 의미 보존 형태로 정리하는 것입니다.

- 기본 정규화
  - 유니코드 정규화: `NFKC`
  - 개행/탭을 공백으로 치환, 연속 공백 축약, trim
  - 한글 자모(호환/조합) 매핑 보정
- HTML/URL/노이즈 처리
  - HTML entity 일부 디코딩, HTML 태그 제거
  - URL 제거
  - 이모지는 `EMOJI` 토큰으로 치환 후 후처리
  - 문자/숫자/공백과 일부 구두점만 남기고 나머지 제거
- 멘션/해시태그 처리
  - `@id` -> `@MENTION`
  - `#tag` -> `#TAG`
- 반복 축약(의미 보존)
  - `ㅋㅋㅋㅋㅋㅋ` -> `ㅋㅋ`
  - `ㅠㅠㅠㅠ` -> `ㅠㅠ`
  - `!!!!` -> `!`
  - `????` -> `?`
  - `......` -> `...`

전처리 결과는 별도 JSONL 파일로 저장되며, 이후 분석은 전처리 파일을 기준으로 수행합니다.

## 3) 키워드 분석(Top N)

- 기법: 빈도 기반(`frequency-v1`)
- 토큰화 후 단어 등장 횟수를 집계하고, 상위 N개를 반환합니다.
- 옵션: 빅그램 사용 시 인접 토큰을 결합한 2-gram도 카운트합니다.
- 결과 저장: `youtube_comment_analysis_top_keywords.result_json`

## 4) 주제별 묶음(Topic Groups)

- 목표: 댓글을 몇 개의 “주제 묶음”으로 나눠 대표 키워드를 보여줍니다.
- 개요:
  - 대표 헤드(시드) 키워드를 뽑고
  - 댓글 단위 동시출현(co-occurrence)로 시드 키워드 세트를 확장
  - 댓글이 어느 시드 세트와 가장 많이 겹치는지로 분류
  - 간단한 반복(EM 형태)으로 시드를 보정
- 결과 저장: `youtube_comment_analysis_topic_groups.result_json`

## 5) 감정분석(사전 기반)

- 기법: 사전 기반 “최대 n-gram 매칭”(`lexicon-max-ngram-v1`)
- 감성사전:
  - KNU 한국어 감성사전 기반 TSV: `/docs/youtubeComment/sentiment_lexicon.tsv`
  - 커스텀 사전 TSV: `/users/youtubeComment/lexicon/custom.tsv`
- 출력:
  - 전체 분포(긍정/중립/부정/미분류)
  - 시간대(3시간 버킷)별 분포 변화(%)
- 저장:
  - 요약 결과(JSON): `youtube_comment_analysis_sentiments.result_json`
  - 댓글 단위 결과(DB): `youtube_comment_analysis_sentiment_items`

## 6) 네트워크 분석(단어 공동출현)

- 기법: 댓글 단위 동시출현 네트워크(`cooccurrence-network-v1`)
- 노드: 단어(토큰)
- 엣지: 같은 댓글에서 함께 등장한 단어 쌍
- 필터:
  - 최소 문서 빈도(minDocFreq), 최소 엣지 가중치(minEdgeWeight) 적용
  - 노드/엣지 개수 제한으로 시각화 과밀 방지
- 결과 저장: `youtube_comment_analysis_word_networks.result_json`

## 7) LLM(Ollama) 재판단/보정(설계)

현재 목표는 “사전 기반 1차 분류” 결과를 바탕으로, 오분류만 선별해 LLM으로 재판단하고 보정값을 DB에 남기는 것입니다.

- 권장 흐름
  - 화면에서 `youtube_comment_analysis_sentiment_items` 목록을 조회
  - 오분류/미분류 중심으로 선택(또는 규칙 기반 후보 추출)
  - Ollama에 댓글 텍스트를 넣어 라벨을 재판단
  - DB 업데이트:
    - `llm_label`, `final_label`, `corrected`, `corrected_reason`
  - 필요 시 커스텀 사전 TSV에 반영 후 재분석

## 참고

- 이력/파일 기반 저장 구조라서 “재분석 수행” 시 같은 history_id의 기존 분석 결과는 삭제 후 재적재됩니다.
