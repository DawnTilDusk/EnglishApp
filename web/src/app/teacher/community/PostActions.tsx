"use client";

import { useActionState } from "react";
import { initialActionState } from "@/lib/action-state";
import {
  deleteCommunityPostAction,
  setCommunityPostFeaturedAction,
} from "./actions";

export function FeaturePostButton({
  postId,
  featured,
}: {
  postId: string;
  featured: boolean;
}) {
  const [state, formAction, pending] = useActionState(
    setCommunityPostFeaturedAction,
    initialActionState
  );

  return (
    <form action={formAction} style={{ display: "inline" }}>
      <input type="hidden" name="post_id" value={postId} />
      <input type="hidden" name="featured" value={featured ? "false" : "true"} />
      <button className="btn secondary" type="submit" disabled={pending}>
        {pending ? "…" : featured ? "取消精华" : "设为精华"}
      </button>
      {state.error && (
        <span className="error" style={{ marginLeft: 8 }}>
          {state.error}
        </span>
      )}
    </form>
  );
}

export function DeletePostButton({ postId }: { postId: string }) {
  const [state, formAction, pending] = useActionState(
    deleteCommunityPostAction,
    initialActionState
  );

  return (
    <form
      action={formAction}
      style={{ display: "inline" }}
      onSubmit={(e) => {
        if (!window.confirm("确定删除该帖？删除后不可恢复。")) {
          e.preventDefault();
        }
      }}
    >
      <input type="hidden" name="post_id" value={postId} />
      <button className="btn secondary" type="submit" disabled={pending}>
        {pending ? "…" : "删除"}
      </button>
      {state.error && (
        <span className="error" style={{ marginLeft: 8 }}>
          {state.error}
        </span>
      )}
    </form>
  );
}
