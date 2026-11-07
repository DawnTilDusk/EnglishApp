"use client";

import { createClient } from "@/lib/supabase/client";
import { FormEvent, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { SeedieMark } from "@/components/SeedieMark";

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
      router.refresh();
      return;
    }
    if (profile?.role === "teacher") {
      router.replace("/teacher");
      router.refresh();
      return;
    }
    await supabase.auth.signOut();
    setError("学生账号请使用 Seedie App 登录。");
    setLoading(false);
  }

  return (
    <main className="login-page">
      <section className="login-story">
        <div className="console-brand"><SeedieMark /><span className="brand-name">Seedie</span></div>
        <span className="eyebrow">TEACHING &amp; GROWING</span>
        <h1>用心教学，<br />看见每一份成长。</h1>
        <p>从一次练习到一段进步。<br />在这里，连接你的学生、课堂与日常。</p>
      </section>
      <div className="card login-card">
        <div className="stack">
          <div>
            <h2>欢迎回来</h2>
            <p className="muted" style={{ margin: 0 }}>
              登录你的机构与教师工作台
            </p>
          </div>
          {banner && <p className="error" role="alert">{banner}</p>}
          {error && <p className="error" role="alert">{error}</p>}
          <form className="stack" onSubmit={onSubmit}>
            <label>
              邮箱
              <input
                type="email"
                autoComplete="username"
                placeholder="输入工作邮箱"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </label>
            <label>
              密码
              <input
                type="password"
                autoComplete="current-password"
                placeholder="输入登录密码"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </label>
            <button className="btn login-submit" type="submit" disabled={loading}>
              {loading ? "正在进入…" : "进入工作台"}
            </button>
          </form>
          <p className="login-note">学生请使用 Seedie Android App 登录。</p>
        </div>
      </div>
    </main>
  );
}
