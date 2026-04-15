# React Hooks Top 5 (예제 포함)

React에서 `useState`, `useEffect` 같은 `use...` 함수들을 **Hook(훅)** 이라고 부릅니다.
Hook은 함수 컴포넌트에 "상태, 렌더 이후 작업, 공유 상태, 외부 값 저장" 같은 기능을 붙여줍니다.

이 문서는 처음 공부할 때 가장 자주 만나는 훅 5개만 정리합니다.

- `useState`
- `useEffect`
- `useRef`
- `useContext`
- `useReducer`

예제는 React + TypeScript 기준입니다.

---

## 0) 핵심 전제: React는 "상태(state) -> UI"다

React에서 UI는 상태로부터 계산됩니다.

- 상태를 `setState`로 바꾸면 React가 다시 렌더링하고 UI가 따라옵니다.
- 그냥 일반 변수(`let x = ...`)를 바꿔도 React는 모르기 때문에 UI가 보통 안 바뀝니다.

---

## 1) useState: 값 저장 + UI 갱신 트리거

언제 쓰나:
- 화면에 보여주는 값, 입력 폼 값, 토글, 선택 값 등 "UI가 따라와야 하는 값"에 씁니다.

예제:

```tsx
import { useState } from "react";

export function Counter() {
  const [count, setCount] = useState(0);

  return (
    <div>
      <div>count: {count}</div>
      <button type="button" onClick={() => setCount((c) => c + 1)}>
        +1
      </button>
    </div>
  );
}
```

포인트:
- `setCount((c) => c + 1)` 처럼 "이전 값 기반" 업데이트는 실수(동시 업데이트) 방지가 됩니다.

---

## 2) useEffect: 렌더 이후 작업(부수효과) + 정리(cleanup)

언제 쓰나:
- 데이터 fetch, 이벤트 리스너 등록, 타이머, 구독 같은 "렌더링 이후에 해야 하는 일"에 씁니다.

### 2-1) 마운트 시 1번만 실행

```tsx
import { useEffect, useState } from "react";

type Todo = { id: number; title: string };

export function Todos() {
  const [todos, setTodos] = useState<Todo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;

    const load = async () => {
      setError(null);
      setLoading(true);
      try {
        const r = await fetch("/api/todos", { credentials: "include" });
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        const j = (await r.json()) as Todo[];
        if (!alive) return;
        setTodos(Array.isArray(j) ? j : []);
      } catch (e) {
        if (!alive) return;
        setError(e instanceof Error ? e.message : String(e));
      } finally {
        if (!alive) return;
        setLoading(false);
      }
    };

    load();
    return () => {
      alive = false;
    };
  }, []); // <-- []: 최초 1회

  if (loading) return <div>Loading...</div>;
  if (error) return <div style={{ color: "crimson" }}>Error: {error}</div>;

  return (
    <ul>
      {todos.map((t) => (
        <li key={t.id}>{t.title}</li>
      ))}
    </ul>
  );
}
```

### 2-2) 의존성 배열(deps)

- 생략: 렌더마다 실행(대부분 필요 없음)
- `[]`: 마운트 시 1번만
- `[a, b]`: `a` 또는 `b`가 바뀔 때 실행

---

## 3) useRef: 값/DOM을 저장하지만 리렌더는 안 함

언제 쓰나:
- DOM에 직접 접근해야 할 때(포커스)
- "값은 저장해야 하지만 UI는 안 바뀌어도 되는" 경우(타이머 id 등)

예제(인풋 포커스):

```tsx
import { useRef } from "react";

export function FocusInput() {
  const inputRef = useRef<HTMLInputElement | null>(null);

  return (
    <div>
      <input ref={inputRef} className="input" />
      <button type="button" onClick={() => inputRef.current?.focus()}>
        Focus
      </button>
    </div>
  );
}
```

포인트:
- `ref.current`를 바꿔도 리렌더가 발생하지 않습니다.

---

## 4) useContext: props 없이도 전역처럼 값 공유

언제 쓰나:
- 로그인 사용자 정보, 테마, 언어 설정 등 "여러 컴포넌트에서 공유하는 값"에 씁니다.

예제(사용자 이름을 전역처럼 읽기):

```tsx
import { createContext, useContext } from "react";

type Me = { username: string };
const MeContext = createContext<Me | null>(null);

export function Profile() {
  const me = useContext(MeContext);
  if (!me) return <div>Not logged in</div>;
  return <div>Hello, {me.username}</div>;
}
```

포인트:
- 실제 앱에서는 `MeContext.Provider value={...}`로 상위에서 값을 내려줘야 합니다.

---

## 5) useReducer: 상태 업데이트 로직을 한 곳에 모으기

언제 쓰나:
- 상태가 여러 필드로 구성되고, 업데이트 케이스가 많아서 `useState`가 복잡해질 때 씁니다.

예제(폼 상태):

```tsx
import { useReducer } from "react";

type State = { email: string; password: string };
type Action =
  | { type: "setEmail"; email: string }
  | { type: "setPassword"; password: string }
  | { type: "reset" };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setEmail":
      return { ...state, email: action.email };
    case "setPassword":
      return { ...state, password: action.password };
    case "reset":
      return { email: "", password: "" };
  }
}

export function LoginForm() {
  const [state, dispatch] = useReducer(reducer, { email: "", password: "" });

  return (
    <form>
      <input
        className="input"
        value={state.email}
        onChange={(e) => dispatch({ type: "setEmail", email: e.target.value })}
        placeholder="email"
      />
      <input
        className="input"
        value={state.password}
        onChange={(e) => dispatch({ type: "setPassword", password: e.target.value })}
        placeholder="password"
        type="password"
      />
      <button type="button" onClick={() => dispatch({ type: "reset" })}>
        Reset
      </button>
    </form>
  );
}
```

---

## (보너스) useMemo / useCallback은 언제?

처음에는 "필요할 때만" 쓰는 게 좋습니다.

- `useMemo`: 계산 비용이 큰 값(필터/정렬/복잡한 계산)을 캐시하고 싶을 때
- `useCallback`: 자식 컴포넌트에 함수를 props로 넘기는데, 불필요한 리렌더를 줄이고 싶을 때

대부분의 성능 문제는 먼저 "렌더가 무거운지"를 확인하고, 그 다음에 적용하는 편이 좋습니다.

