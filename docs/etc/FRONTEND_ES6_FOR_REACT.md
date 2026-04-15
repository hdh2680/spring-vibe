# React를 위한 ES6 (실전 치트시트)

React 자체가 어려운 게 아니라, 막히는 지점이 ES6 문법인 경우가 많습니다. 특히 "짧게 쓰는 문법"과 "불변 업데이트"가 합쳐지면 코드가 낯설게 보이기 쉬워요.

이 문서는 React 코드에서 매일 보게 되는 ES6(+) 문법만 실전 예제로 정리합니다.

---

## 1) 구조분해 할당(객체, 배열)

### props/객체에서 꺼내기

```ts
function Profile(props: { name: string; age: number }) {
  const { name, age } = props;
  return <div>{name} ({age})</div>;
}
```

파라미터에서 바로 구조분해도 많이 합니다:

```ts
function Profile({ name, age }: { name: string; age: number }) {
  return <div>{name} ({age})</div>;
}
```

### useState는 배열 구조분해(필수)

```ts
const [value, setValue] = useState("");
```

---

## 2) 스프레드/레스트(`...`)

### 복사하고 필드 추가/덮어쓰기(불변 업데이트)

```ts
setUser((prev) => ({ ...prev, nickname: "neo" }));
```

### 배열: 추가/앞에 추가/삭제

```ts
setTags((prev) => [...prev, "react"]);
setTags((prev) => ["react", ...prev]);
setTags((prev) => prev.filter((t) => t !== "legacy"));
```

### 레스트 파라미터(남은 인자 모으기)

```ts
function join(...parts: string[]) {
  return parts.join("/");
}
```

---

## 3) 화살표 함수

### 이벤트 핸들러

```tsx
<button type="button" onClick={() => setOpen(true)}>
  Open
</button>
```

### 함수형 state 업데이트(스테일 state 방지)

```ts
setCount((c) => c + 1);
```

### 주의: 객체 리터럴을 반환할 때

```ts
const ok = () => ({ a: 1 }); // parentheses required
```

---

## 4) 템플릿 리터럴

```ts
const url = `/api/users/${userId}/posts?limit=${limit}`;
```

---

## 5) 기본 파라미터

```ts
function clamp(n: number, min = 0, max = 100) {
  return Math.min(max, Math.max(min, n));
}
```

---

## 6) optional chaining(`?.`)과 nullish coalescing(`??`)

데이터 로딩 전(null/undefined)에 UI가 터지지 않게 하는 데 매우 자주 씁니다.

```ts
const displayName = user?.profile?.name ?? "Anonymous";
```

`||`와 `??` 차이:

```ts
0 || 10;   // 10 (because 0 is falsy)
0 ?? 10;   // 0  (because 0 is not null/undefined)
"" || "x"; // "x"
"" ?? "x"; // ""
```

---

## 7) 조건부 렌더링: 단락 평가와 삼항 연산자

```tsx
{error && <p className="error">{error}</p>}
{loading ? <Spinner /> : <List items={items} />}
```

---

## 8) 배열 메서드(React는 배열을 렌더링한다)

### `map` (리스트 렌더링)

```tsx
<ul>
  {items.map((it) => (
    <li key={it.id}>{it.title}</li>
  ))}
</ul>
```

### `filter` (삭제/필터링)

```ts
const visible = items.filter((it) => it.enabled);
```

### `find` (하나 찾기)

```ts
const selected = items.find((it) => it.id === selectedId) ?? null;
```

### `reduce` (합계/집계)

```ts
const total = cart.reduce((sum, item) => sum + item.price, 0);
```

### `sort` (중요: 원본 배열을 변경함)

```ts
// Never mutate state arrays. Copy first.
const sorted = [...items].sort((a, b) => a.name.localeCompare(b.name));
```

---

## 9) 모듈: `export` / `import`

```ts
// a.ts
export function f() {}
export const X = 1;
export default function Main() {}

// b.ts
import Main, { f, X } from "./a";
```

---

## 10) Promise와 `async/await` (데이터 fetch)

```ts
async function load() {
  const r = await fetch("/api/items", { credentials: "include" });
  if (!r.ok) throw new Error(`HTTP ${r.status}`);
  return (await r.json()) as unknown;
}
```

React effect 안에서는 언마운트 이후 setState를 막는 패턴을 자주 씁니다:

```ts
useEffect(() => {
  let alive = true;
  (async () => {
    const data = await load();
    if (!alive) return;
    setItems(Array.isArray(data) ? data : []);
  })();
  return () => {
    alive = false;
  };
}, []);
```

---

## 11) React에서 특히 중요한 습관: 불변성(immutability)

state 업데이트는 "새 참조"를 만들어야 합니다. 이런 코드는 피하세요:

```ts
// bad: mutates the same array reference
state.items.push(x);
setState(state);
```

이렇게 합니다:

```ts
setState((prev) => ({ ...prev, items: [...prev.items, x] }));
```

---

## 12) 미니 연습(5분)

1) 중첩 필드 불변 업데이트

```ts
type State = { user: { profile: { name: string } } };
// TODO: implement setName("kim")
```

2) id로 삭제하기

```ts
type Item = { id: string; title: string };
// TODO: setItems((prev) => ...)
```

3) 렌더링 시 기본값 넣기

```tsx
// TODO: title = it.title ?? "(untitled)"
```
