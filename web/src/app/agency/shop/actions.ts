"use server";

import { createClient } from "@/lib/supabase/server";
import { revalidatePath } from "next/cache";
import type { ActionState } from "@/lib/action-state";

export async function upsertProductAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const id = String(formData.get("id") || "");
  const name = String(formData.get("name") || "").trim();
  const description = String(formData.get("description") || "").trim() || null;
  const priceTokens = Number(formData.get("price_tokens") || 0);
  const stock = Number(formData.get("stock") || -1);
  if (!name || !Number.isFinite(priceTokens) || priceTokens < 1) {
    return { error: "请填写有效的名称与价格。", ok: false };
  }

  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return { error: "Not authenticated", ok: false };

  const { data: profile } = await supabase
    .from("profiles")
    .select("agency_id, role")
    .eq("id", user.id)
    .single();

  if (profile?.role !== "agency_admin" || !profile.agency_id) {
    return { error: "仅机构管理员可管理商品。", ok: false };
  }

  if (id) {
    const { error } = await supabase
      .from("shop_products")
      .update({
        name,
        description,
        price_tokens: priceTokens,
        stock,
      })
      .eq("id", id)
      .eq("agency_id", profile.agency_id);
    if (error) return { error: error.message, ok: false };
  } else {
    const { error } = await supabase.from("shop_products").insert({
      agency_id: profile.agency_id,
      name,
      description,
      price_tokens: priceTokens,
      stock,
      is_active: true,
    });
    if (error) return { error: error.message, ok: false };
  }

  revalidatePath("/agency/shop");
  revalidatePath("/agency");
  return { error: null, ok: true };
}

export async function setProductActiveAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const id = String(formData.get("id") || "");
  const nextActive = String(formData.get("is_active") || "") === "true";

  if (!id) return { error: "Missing product id", ok: false };

  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return { error: "Not authenticated", ok: false };

  const { data: profile } = await supabase
    .from("profiles")
    .select("agency_id, role")
    .eq("id", user.id)
    .single();

  if (profile?.role !== "agency_admin" || !profile.agency_id) {
    return { error: "仅机构管理员可上下架商品。", ok: false };
  }

  const { error } = await supabase
    .from("shop_products")
    .update({ is_active: nextActive })
    .eq("id", id)
    .eq("agency_id", profile.agency_id);

  if (error) return { error: error.message, ok: false };

  revalidatePath("/agency/shop");
  revalidatePath("/agency");
  return { error: null, ok: true };
}
