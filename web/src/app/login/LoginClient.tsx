"use client";

import { createClient } from "@/lib/supabase/client";
import { FormEvent, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

export default function LoginClient() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const banner = useMemo(() => {
    const code = searchParams.get("error");
    if (code === "student_use_app") return "学生账号请使用 Seedie App 登录。";
    if (code === "profile") return "无法读取账号档案，请联系管理员。";
    return null;
  }, [searchParams]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    const supabase = createClient();
    const { data, error: signError } = await supabase.auth.signInWithPassword({
      email,
      password,
    });
    if (signError || !data.user) {
      setError(signError?.message ?? "登录失败");
      setLoading(false);
      return;
    }

    const { data: profile } = await supabase
      .from("profiles")
      .select("role")
      .eq("id", data.user.id)
      .maybeSingle();

    if (profile?.role === "agency_admin") {
      router.replace("/agency");
    } else if (profile?.role === "teacher") {
      router.replace("/teacher");
    } else {
      await supabase.auth.signOut();
      setError("学生账号请使用 Seedie App 登录。");
    }
    setLoading(false);
  }

  return (
    <main>
      <div className="card" style={{ maxWidth: 420, margin: "64px auto" }}>
        <div className="stack">
          <div>
            <h1 style={{ margin: "0 0 8px" }}>Seedie Console</h1>
            <p className="muted" style={{ margin: 0 }}>
              机构与教师网页端
            </p>
          </div>
          {banner && <p className="error">{banner}</p>}
          {error && <p className="error">{error}</p>}
          <form className="stack" onSubmit={onSubmit}>
            <label>
              邮箱
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </label>
            <label>
              密码
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </label>
            <button className="btn" type="submit" disabled={loading}>
              {loading ? "登录中…" : "登录"}
            </button>
          </form>
        </div>
      </div>
    </main>
  );
}
