"use client";

import { useState, useTransition } from "react";

type ActionResult = { error: string | null };

type ProductInput = {
  id?: string;
  name?: string;
  description?: string | null;
  price_tokens?: number;
  stock?: number;
  is_active?: boolean;
};

export function ProductForm({
  action,
  product,
  compact = false,
}: {
  action: (formData: FormData) => Promise<ActionResult>;
  product?: ProductInput;
  compact?: boolean;
}) {
  const [error, setError] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  return (
    <form
      className="stack"
      onSubmit={(e) => {
        e.preventDefault();
        const formData = new FormData(e.currentTarget);
        startTransition(async () => {
          const result = await action(formData);
          setError(result.error);
          if (!result.error && !product?.id) {
            e.currentTarget.reset();
          }
        });
      }}
    >
      {error && <p className="error">{error}</p>}
      {product?.id && <input type="hidden" name="id" value={product.id} />}
      <label>
        名称
        <input name="name" defaultValue={product?.name} required />
      </label>
      {!compact && (
        <label>
          描述
          <textarea name="description" defaultValue={product?.description ?? ""} rows={3} />
        </label>
      )}
      {compact && (
        <input type="hidden" name="description" value={product?.description ?? ""} />
      )}
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
      <label className="row" style={{ justifyContent: "flex-start" }}>
        <input
          name="is_active"
          type="checkbox"
          defaultChecked={product?.is_active ?? true}
        />
        上架
      </label>
      <button className="btn" type="submit" disabled={pending}>
        {pending ? "保存中…" : product?.id ? "更新" : "创建"}
      </button>
    </form>
  );
}
