import Link from "next/link";
import { signOutAction } from "@/app/actions";
import type { Profile } from "@/lib/types";
import { SeedieMark } from "@/components/SeedieMark";

export function ConsoleShell({
  profile,
  title,
  tabs,
  children,
}: {
  profile: Profile;
  title: string;
  tabs: { href: string; label: string; active?: boolean }[];
  children: React.ReactNode;
}) {
  return (
    <main className="console-shell">
      <div className="console-brand">
        <SeedieMark />
        <span className="brand-name">Seedie</span>
        <span className="brand-caption">让教学与成长，清晰相连</span>
      </div>
      <div className="nav">
        <div>
          <h1>{title}</h1>
          <p className="account-summary">
            {profile.display_name || profile.email}<span className="role-pill">{profile.role === "teacher" ? "教师工作台" : "机构管理"}</span>
          </p>
        </div>
        <form action={signOutAction}>
          <button className="btn secondary" type="submit">
            退出
          </button>
        </form>
      </div>
      <nav className="tabs" aria-label="控制台导航">
        {tabs.map((tab) => (
          <Link
            key={tab.href}
            href={tab.href}
            className={tab.active ? "active" : undefined}
            aria-current={tab.active ? "page" : undefined}
          >
            {tab.label}
          </Link>
        ))}
      </nav>
      <section className="console-content">{children}</section>
    </main>
  );
}
