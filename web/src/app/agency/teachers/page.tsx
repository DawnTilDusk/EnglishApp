import { requireAgencyAdmin } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import { agencyTabs } from "@/lib/agency-tabs";
import { CreateTeacherForm } from "./CreateTeacherForm";

export default async function AgencyTeachersPage() {
  const profile = await requireAgencyAdmin();
  const supabase = await createClient();
  const { data: teachers } = await supabase
    .from("teachers")
    .select("id, display_name, agency_id")
    .eq("agency_id", profile.agency_id!)
    .order("display_name");

  return (
    <ConsoleShell
      profile={profile}
      title="教师管理"
      tabs={agencyTabs("teachers")}
    >
      <div className="stack">
        <div className="card">
          <h2 style={{ marginTop: 0 }}>创建教师账号</h2>
          <CreateTeacherForm />
        </div>
        <div className="card">
          <h2 style={{ marginTop: 0 }}>本机构教师</h2>
          <table>
            <thead>
              <tr>
                <th>姓名</th>
                <th>ID</th>
              </tr>
            </thead>
            <tbody>
              {(teachers ?? []).map((t) => (
                <tr key={t.id}>
                  <td>{t.display_name}</td>
                  <td className="muted">{t.id}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </ConsoleShell>
  );
}
