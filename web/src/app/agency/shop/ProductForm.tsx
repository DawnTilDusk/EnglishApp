"use client";

import { useActionState, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { initialActionState } from "@/lib/action-state";
import { upsertProductAction } from "./actions";

type ProductInput = {
  id?: string;
  name?: string;
  description?: string | null;
  price_tokens?: number;
  stock?: number;
};

export function ProductForm({
  product,
  statusFilter = "all",
  cancelHref,
}: {
  product?: ProductInput;
  statusFilter?: string;
  cancelHref?: string;
}) {
  const [state, formAction, pending] = useActionState(
    upsertProductAction,
    initialActionState
  );
  const formRef = useRef<HTMLFormElement>(null);
  const router = useRouter();
  const isEdit = Boolean(product?.id);

  useEffect(() => {
    if (!state.ok) return;
    if (isEdit && cancelHref) {
      router.replace(cancelHref);
    } else {
      formRef.current?.reset();
    }
  }, [state, isEdit, cancelHref, router]);

  return (
    <form ref={formRef} className="stack" action={formAction}>
      {state.error && <p className="error">{state.error}</p>}
      {product?.id && <input type="hidden" name="id" value={product.id} />}
      <input type="hidden" name="status_filter" value={statusFilter} />
      <label>
        名称
        <input name="name" defaultValue={product?.name} required />
      </label>
      <label>
        描述
        <textarea
          name="description"
          defaultValue={product?.description ?? ""}
          rows={3}
        />
      </label>
      <div className="row">
        <label style={{ flex: 1 }}>
          价格（代币）
          <input
            name="price_tokens"
            type="number"
            min={1}
            defaultValue={product?.price_tokens ?? 10}
            required
          />
        </label>
        <label style={{ flex: 1 }}>
          库存（-1 不限）
          <input
            name="stock"
            type="number"
            defaultValue={product?.stock ?? -1}
            required
          />
        </label>
      </div>
      <div className="row">
        <button className="btn" type="submit" disabled={pending}>
          {pending ? "保存中…" : isEdit ? "保存修改" : "新增商品"}
        </button>
        {isEdit && cancelHref && (
          <button
            className="btn secondary"
            type="button"
            onClick={() => router.push(cancelHref)}
          >
            取消
          </button>
        )}
      </div>
    </form>
  );
}
