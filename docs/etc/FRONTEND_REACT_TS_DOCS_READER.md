# React TS 문서 뷰어(ReactTsDocsPage.tsx) 소스 설명

이 문서는 React를 처음 접하는 사람을 기준으로, 아래 파일이 어떤 흐름으로 동작하는지 설명합니다.

- `frontend/src/features/labs/ReactTsDocsPage.tsx`

이 페이지의 역할은 간단합니다.

1. 서버에서 문서 목록을 가져와 드롭다운에 보여준다.
2. 사용자가 선택한 문서의 내용을 가져와 화면에 렌더링한다.

---

## 1) ReactTsDocsPage가 들고 있는 상태(state)

React의 `useState`는 "컴포넌트가 기억하는 값"입니다. `setXxx(...)`로 값을 바꾸면 자동으로 다시 렌더링됩니다.

이 파일에서는 다음 상태를 사용합니다.

- `items`: 문서 목록(드롭다운 옵션들)
- `selectedId`: 현재 선택된 문서 id
- `doc`: 선택된 문서의 본문 데이터
- `loading`: 네트워크 로딩 중인지 여부
- `error`: 실패했을 때 보여줄 에러 메시지

`doc`은 없을 수도 있으니 `null`을 허용합니다.

---

## 2) 최초 1회 로딩(useEffect with `[]`)

`useEffect(() => { ... }, [])`는 "처음 화면이 뜬 다음 1번" 실행됩니다.

이 effect에서 하는 일:

1. `GET /api/docs/etc`로 문서 목록을 가져온다.
2. 목록이 비어있지 않으면 첫 문서를 자동으로 선택한다.
3. 선택된 문서 id로 `GET /api/docs/etc/{id}`를 호출해서 문서를 가져온다.

중간중간 `loading`, `error` 상태를 업데이트해서 UI가 "Loading..." 또는 에러 메시지를 보여줄 수 있게 합니다.

> 참고: 코드에 있는 `alive` 플래그는 "컴포넌트가 사라진 뒤(setState 하면 경고가 날 수 있음)"를 방지하려는 안전장치입니다.

---

## 3) 문서 선택 변경 로딩(useEffect with `[selectedId]`)

`useEffect(() => { ... }, [selectedId])`는 `selectedId`가 바뀔 때마다 실행됩니다.

드롭다운을 바꾸면:

1. `onChange`에서 `setSelectedId(...)`가 호출됨
2. `selectedId`가 바뀌면서 effect가 실행됨
3. `GET /api/docs/etc/{selectedId}`로 새 문서를 받아서 `doc`을 바꿈

코드에 있는 조건:

```ts
if (selectedId && doc?.id !== selectedId) {
  loadDoc();
}
```

이건 "최초 로딩 effect에서 이미 첫 문서를 가져왔으니, 마운트 직후 중복 호출은 하지 말자"는 의도입니다.

---

## 4) 서버 API 응답 형태(`/api/docs/etc`)

### 4-1) 문서 목록: `GET /api/docs/etc`

응답(JSON) 예시:

```json
[
  { "id": "react-vite-guide", "title": "Frontend (React + TypeScript, Vite) 병행 운영 가이드" },
  { "id": "react-ts-docs-reader", "title": "React TS 문서 뷰어(ReactTsDocsPage.tsx) 소스 설명" }
]
```

프론트는 이 값을 `items`에 저장하고, `items.map(...)`으로 `<option>`을 만듭니다.

### 4-2) 문서 1개: `GET /api/docs/etc/{id}`

응답은 대략 아래 필드를 포함합니다.

- `id`: 문서 id
- `title`: 문서 제목
- `markdown`: 원본 마크다운 텍스트
- `html`: 서버에서 마크다운을 렌더링(변환)한 HTML

응답(JSON) 예시(형태만 이해용):

```json
{
  "id": "react-ts-docs-reader",
  "title": "React TS 문서 뷰어(ReactTsDocsPage.tsx) 소스 설명",
  "markdown": "# ...",
  "html": "<h1>...</h1>"
}
```

프론트는 `doc.html`을 화면에 그립니다.

---

## 5) HTML 예시와 `dangerouslySetInnerHTML`

React는 보통 JSX로 화면을 그리지만, 이 페이지는 서버에서 내려준 HTML 문자열을 그대로 DOM에 꽂습니다.

```tsx
<div className=\"markdown\" dangerouslySetInnerHTML={{ __html: doc?.html ?? \"\" }} />
```

### 5-1) 마크다운 입력 예시

예를 들어 서버가 아래 마크다운을 렌더링한다고 가정하면:

```md
# 제목

**굵게**, `인라인 코드`

- 리스트 1
- 리스트 2
```

### 5-2) 렌더링된 HTML 예시(개념용)

서버 렌더러가 대략 이런 HTML을 만들 수 있습니다(실제 결과는 렌더러/옵션에 따라 조금씩 다를 수 있음).

```html
<h1>제목</h1>
<p><strong>굵게</strong>, <code>인라인 코드</code></p>
<ul>
  <li>리스트 1</li>
  <li>리스트 2</li>
</ul>
```

### 5-3) 보안 주의(XSS)

`dangerouslySetInnerHTML`는 말 그대로 위험할 수 있으니, `doc.html`은 반드시 서버에서 "안전한 HTML"로 sanitize(스크립트 제거 등)된 상태여야 합니다.

