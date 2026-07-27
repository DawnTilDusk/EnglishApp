"use client";

import { useActionState } from "react";
import { initialActionState } from "@/lib/action-state";
import { setProductActiveAction } from "./actions";

export function ToggleActiveButton({
  productId,
  isActive,
}: {
  productId: string;
  isActive: boolean;
}) {
  const [state, formAction, pending] = useActionState(
    setProductActiveAction,
    initialActionState
  );

  return (
    <form action={formAction} className="stack" style={{ gap: 4 }}>
      <input type="hidden" name="id" value={productId} />
      <input type="hidden" name="is_active" value={isActive ? "false" : "true"} />
      <button className="btn secondary" type="submit" disabled={pending}>
        {pending ? "…" : isActive ? "下架" : "上架"}
      </button>
      {state.error && <span className="error">{state.error}</span>}
    </form>
  );
}
