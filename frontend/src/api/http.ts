export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

type Json = Record<string, unknown>;

export async function apiJson<T>(
  method: HttpMethod,
  path: string,
  body?: Json
): Promise<T> {
  const res = await fetch(path, {
    method,
    headers: {
      "Content-Type": "application/json"
    },
    body: body ? JSON.stringify(body) : undefined,
    credentials: "include"
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`API ${method} ${path} failed: ${res.status} ${text}`.trim());
  }

  return (await res.json()) as T;
}

