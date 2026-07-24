import Link from "next/link";
import { signOutAction } from "@/app/actions";
import type { Profile } from "@/lib/types";

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
    <main>
      <div className="nav">
        <div>
          <h1>{title}</h1>
          <p className="muted" style={{ margin: "4px 0 0" }}>
            {profile.display_name || profile.email} · {profile.role}
          </p>
        </div>
        <form action={signOutAction}>
          <button className="btn secondary" type="submit">
            退出
          </button>
        </form>
      </div>
      <div className="tabs" style={{ marginBottom: 20 }}>
        {tabs.map((tab) => (
          <Link
            key={tab.href}
            href={tab.href}
            className={tab.active ? "active" : undefined}
          >
            {tab.label}
          </Link>
        ))}
      </div>
      {children}
    </main>
  );
}
