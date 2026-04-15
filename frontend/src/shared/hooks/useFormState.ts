import type { ChangeEvent } from "react";
import { useCallback, useRef, useState } from "react";

type ChangeEl = HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement;

type BindValueOptions<TValue> = {
  parse?: (raw: string) => TValue;
  format?: (value: TValue) => string;
};

export function useFormState<T extends Record<string, unknown>>(initial: T) {
  const initialRef = useRef(initial);
  const [values, setValues] = useState<T>(initial);

  const reset = useCallback(() => setValues(initialRef.current), []);

  const setField = useCallback(<K extends keyof T>(key: K, value: T[K]) => {
    setValues((s) => ({ ...s, [key]: value }));
  }, []);

  const bindValue = useCallback(
    <K extends keyof T>(key: K, opts?: BindValueOptions<T[K]>) => {
      const value = values[key];
      const formatted = opts?.format ? opts.format(value as T[K]) : (value ?? "");

      return {
        name: String(key),
        value: formatted as unknown as string,
        onChange: (e: ChangeEvent<ChangeEl>) => {
          const raw = e.target.value;
          const next = (opts?.parse ? opts.parse(raw) : (raw as unknown as T[K])) as T[K];
          setField(key, next);
        },
      };
    },
    [setField, values],
  );

  const bindChecked = useCallback(
    <K extends keyof T>(key: K) => {
      const checked = Boolean(values[key]);
      return {
        name: String(key),
        checked,
        onChange: (e: ChangeEvent<HTMLInputElement>) => {
          setField(key, e.target.checked as unknown as T[K]);
        },
      };
    },
    [setField, values],
  );

  return { values, setValues, setField, reset, bindValue, bindChecked };
}
