# React 핵심 정리 (State, Props, Hooks, Forms)

실무/토이에서 매일 쓰는 React의 핵심만, 바로 복붙 가능한 패턴 위주로 정리했습니다. JSX/TSX 문법 자체는 이미 안다고 가정합니다.

---

## 1) 멘탈 모델: UI = f(state)

React는 state로 UI를 그립니다. UI가 안 바뀐다면 보통 아래 중 하나입니다:

- 값이 React state에 없다.
- state를 "새로 만들지" 않고 원본을 변경했다(불변성 위반).
- 다른 state를 업데이트하고 있다.

---

## 2) Props vs State

Props: 부모가 내려주는 입력값.

```tsx
function Hello({ name }: { name: string }) {
  return <div>Hello {name}</div>;
}
```

State: 컴포넌트가 소유하는 데이터.

```tsx
function Counter() {
  const [count, setCount] = useState(0);
  return <button onClick={() => setCount((c) => c + 1)}>{count}</button>;
}
```

---

## 3) 리스트 렌더링과 key

```tsx
{todos.map((t) => (
  <TodoRow key={t.id} todo={t} />
))}
```

규칙:

- key는 안정적이고 유니크해야 합니다(가능하면 DB id).
- 진짜로 고정된 리스트가 아니면 index를 key로 쓰지 마세요.

---

## 4) state 업데이트(불변성)

객체 업데이트:

```ts
setUser((prev) => ({ ...prev, nickname: "neo" }));
```

배열 업데이트:

```ts
setItems((prev) => prev.filter((x) => x.id !== id));
setItems((prev) => [...prev, newItem]);
```

중첩 업데이트:

```ts
setState((prev) => ({
  ...prev,
  user: {
    ...prev.user,
    profile: {
      ...prev.user.profile,
      name: "kim",
    },
  },
}));
```

---

## 5) useEffect: 사이드이펙트와 데이터 fetch

대표 케이스:

- 마운트 이후 데이터 로딩
- 구독/해제(이벤트, 소켓, interval)
- 외부 시스템과 동기화

최소 fetch 패턴:

```tsx
type Item = { id: string; title: string };

function Items() {
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    (async () => {
      setError(null);
      setLoading(true);
      try {
        const r = await fetch("/api/items", { credentials: "include" });
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        const j = (await r.json()) as Item[];
        if (!alive) return;
        setItems(Array.isArray(j) ? j : []);
      } catch (e) {
        if (!alive) return;
        setError(e instanceof Error ? e.message : String(e));
      } finally {
        if (!alive) return;
        setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, []);

  if (loading) return <div>Loading...</div>;
  if (error) return <div style={{ color: "crimson" }}>Error: {error}</div>;
  return <pre>{JSON.stringify(items, null, 2)}</pre>;
}
```

---

## 6) 폼: controlled vs uncontrolled

### Controlled (state로 제어)

```tsx
function EmailForm() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const ok = email.includes("@");
    setError(ok ? null : "Invalid email");
    if (!ok) return;
    // submit using `email` state
  };

  return (
    <form onSubmit={onSubmit}>
      <input
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="you@example.com"
        inputMode="email"
      />
      {error ? <div className="error">{error}</div> : null}
      <button type="submit">Submit</button>
    </form>
  );
}
```

노트:

- controlled 폼에서는 state로 제출하니 `name`이 필수는 아닙니다.
- `FormData`를 쓰거나 `react-hook-form` 같은 라이브러리를 쓰면 `name`이 사실상 키가 됩니다.

### Uncontrolled (DOM에서 직접 읽기)

```tsx
function UncontrolledEmailForm() {
  const ref = useRef<HTMLInputElement | null>(null);
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const email = ref.current?.value ?? "";
        // submit email
      }}
    >
      <input ref={ref} name="email" />
      <button type="submit">Submit</button>
    </form>
  );
}
```

---

## 7) 프론트 유효성검사 옵션

React 코어는 유효성검사 프레임워크를 따로 제공하지 않습니다.

- HTML5 검증: `required`, `minLength`, `pattern`, `type="email"`
- state로 직접 검증(위 예시)
- 라이브러리: `react-hook-form` + `zod` 조합이 흔함

---

## 8) 자주 터지는 포인트 체크리스트

- `sort`, `push`, `splice` 같은 변이 메서드로 state를 망가뜨림.
- 굳이 state에 저장할 필요 없는 파생값(가능하면 계산으로 해결).
- deps 누락, 또는 effect로 할 일이 아닌 걸 effect로 처리함(클릭 핸들러로 옮겨야 하는데).
- key가 불안정해서 UI가 엉킴.
