import { useEffect, useState } from "react";

type DocItem = { id: string; title: string };
type Doc = { id: string; title: string; html: string };

export default function ReactTsDocsPage() {
  const [items, setItems] = useState<DocItem[]>([]);
  const [selectedId, setSelectedId] = useState<string>("");
  const [doc, setDoc] = useState<Doc | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    let alive = true;

    const load = async () => {
      setError(null);
      setLoading(true);
      try {
        const listRes = await fetch("/api/docs/etc", { credentials: "include" });
        if (!listRes.ok) throw new Error(`list failed: ${listRes.status}`);
        const list = (await listRes.json()) as DocItem[];
        if (!alive) return;
        const safeList = Array.isArray(list) ? list : [];
        setItems(safeList);

        const firstId = safeList[0]?.id ?? "";
        setSelectedId(firstId);

        if (firstId) {
          const r = await fetch(`/api/docs/etc/${encodeURIComponent(firstId)}`, { credentials: "include" });
          if (!r.ok) throw new Error(`doc ${firstId} failed: ${r.status}`);
          const j = (await r.json()) as Doc;
          if (!alive) return;
          setDoc(j);
        } else {
          setDoc(null);
        }
      } catch (e) {
        if (!alive) return;
        setError(e instanceof Error ? e.message : String(e));
        setDoc(null);
      }
      if (!alive) return;
      setLoading(false);
    };

    load();
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    let alive = true;
    const loadDoc = async () => {
      if (!selectedId) return;
      setError(null);
      setLoading(true);
      try {
        const r = await fetch(`/api/docs/etc/${encodeURIComponent(selectedId)}`, { credentials: "include" });
        if (!r.ok) throw new Error(`doc ${selectedId} failed: ${r.status}`);
        const j = (await r.json()) as Doc;
        if (!alive) return;
        setDoc(j);
      } catch (e) {
        if (!alive) return;
        setError(e instanceof Error ? e.message : String(e));
        setDoc(null);
      }
      if (!alive) return;
      setLoading(false);
    };

    // First load already fetches the initial doc; avoid duplicate fetch on mount.
    // Only fetch when the user changes selection.
    if (selectedId && doc?.id !== selectedId) {
      loadDoc();
    }

    return () => {
      alive = false;
    };
  }, [selectedId]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div>
      <div className="topbar">
        <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between", gap: 12, width: "100%" }}>
          <div>
            <h2 style={{ margin: 0 }}>React TS - Lab Docs</h2>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 10, minWidth: 420 }}>
            <span className="muted" style={{ fontWeight: 850, whiteSpace: "nowrap" }}>
              문서 선택:
            </span>
            <select
              className="input"
              value={selectedId}
              onChange={(e) => setSelectedId(e.target.value)}
              disabled={items.length === 0}
              aria-label="문서 선택"
              style={{ width: "100%" }}
            >
              {items.map((it) => (
                <option key={it.id} value={it.id}>
                  {it.title}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <section className="panel" style={{ marginTop: 12 }}>
        <div className="panel-inner">
          <div className="toolbar" style={{ marginBottom: 8 }}>
            <h3 style={{ margin: 0 }}>{doc?.title ?? "문서"}</h3>
            {loading ? <span className="muted">Loading...</span> : null}
          </div>

          {error ? (
            <p className="muted" style={{ color: "rgba(185, 28, 28, 0.92)" }}>
              로딩 실패: {error}
            </p>
          ) : null}

          {/* NOTE: doc.html must be sanitized server-side to prevent XSS. */}
          <div className="markdown" dangerouslySetInnerHTML={{ __html: doc?.html ?? "" }} />
        </div>
      </section>
    </div>
  );
}
