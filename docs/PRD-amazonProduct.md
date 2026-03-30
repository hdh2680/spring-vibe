# PRD - amazon 제품(Amazon Product)

## 1. 목적(Goals)
- 제품을 빠르게 탐색하고, 키워드/태그 기반으로 원하는 제품을 찾는다.
- DB 기반 기본 목록 + ES 기반 검색을 병행하여, ES 장애/미사용 환경에서도 최소 기능(목록/상세/간단 검색)이 유지된다.

## 2. 구성
- DB : 카테고리명, 제품명은 NVL(한글명, 영문명)으로 처리함.
  - 카테고리 : `amazon_category`
  - 제품 : `amazon_product`
- 패키지 : `src/main/java/springVibe/dev/users/amazonProduct`
- 매핑
  - 목록조회: `/list`
- 화면: `templates/html/users/amazonProduct/`
  - 목록 : `/list.html`

## 3. 화면구성
- 제품 정렬은 별점, 리뷰수 정렬을 기본으로 하고, 목록 우측상단에 별점순, 리뷰순, 가격순, 제품명순 정렬을 제공한다.
- 검색은 카테고리, 제품명으로 한다.
- 제품은 상/하단 으로 레이아웃 구성하여 상단에는 이미지(`img_url`), 하단에는 제품명(`title`, `product_name_ko`), 별점(`stars`), 리뷰수(`reviews`), 가격(`price`), 베스트셀러여부(`is_best_seller`)를 제공한다.
- 제품에 상세보기 시 새창으로 팝업화면을 호출한다. URL(`product_url`) 

## 4. 검색(ES)
- `title` 색인 예정임.
- 검색어 한글 입력시 ollama LLM 통해서 영문으로 번역, 번역된 결과를 ES에 요청
- `application-local.yml`의 `dev-search.elasticsearch.enabled` 환경변수 false 일때는 번역내용을 DB LIKE검색함.


## 비고
- amazon 데이터라서 영문제품명으로 되어있다. **ollama를 설치, 도커로 실행**해서 140만건 번역 진행
- ollama로 번역진행시 200건/10분 속도나와서 사용못함.
- 추후방향 : 사용자 검색어 한글로 입력 시 해당 문자를 ollama 활용하여 영문으로 번역해서 검색