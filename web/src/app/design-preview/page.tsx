import { notFound } from "next/navigation";
import { ConsoleShell } from "@/components/ConsoleShell";

/** A local visual fixture; production returns 404 and never exposes student data. */
export default function DesignPreview() {
  if (process.env.NODE_ENV !== "development") notFound();
  return (
    <ConsoleShell title="教学工作台"
      profile={{ id: "visual-fixture", role: "teacher", agency_id: null, display_name: "界面预览", email: null }}
      tabs={[{ href: "#overview", label: "学习概览", active: true }, { href: "#students", label: "我的学生" },
        { href: "#assignments", label: "练习作业" }, { href: "#feedback", label: "写作反馈" }]}>
      <p className="muted" style={{ fontSize: 12, marginBottom: 0 }}>设计预览 · 以下均为示例数据，仅用于检查排版，未读取业务数据。</p>
      <div className="row" id="overview">
        {[ ["本周参与学生", "24", "保持学习的每一小步"], ["进行中的作业", "3", "有序安排学习节奏"],
          ["待反馈作品", "6", "让努力获得回应"] ].map(([label, value, note]) => (
          <div className="card" key={label} style={{ flex: 1, minWidth: 200 }}>
            <div className="muted" style={{ fontSize: 13 }}>{label}</div>
            <div style={{ fontSize: 36, lineHeight: 1.7, fontWeight: 650, letterSpacing: "-.04em" }}>{value}</div>
            <div className="muted" style={{ fontSize: 12 }}>{note}</div>
          </div>
        ))}
      </div>
      <div className="card stack" id="assignments">
        <div className="row" style={{ justifyContent: "space-between" }}>
          <div><h2 style={{ marginBottom: 5 }}>近期练习</h2><p className="muted" style={{ margin: 0, fontSize: 13 }}>把关注留给需要帮助的地方。</p></div>
          <button className="btn" type="button" disabled>布置练习 · 示例</button>
        </div>
        <table>
          <thead><tr><th>练习内容</th><th>模块</th><th>学习状态</th><th>进度</th></tr></thead>
          <tbody>
            <tr><td>安静阅读桌</td><td>阅读训练</td><td><span className="role-pill" style={{ marginLeft: 0 }}>进行中</span></td><td>18 / 24</td></tr>
            <tr><td>国际象棋社换教室</td><td>听力训练</td><td><span className="role-pill" style={{ marginLeft: 0 }}>进行中</span></td><td>21 / 24</td></tr>
            <tr><td>给未来的一封信</td><td>写作实战</td><td><span className="muted">待反馈</span></td><td>6 份作品</td></tr>
          </tbody>
        </table>
      </div>
      <div className="card" id="feedback"><h3 style={{ marginBottom: 8 }}>从反馈开始下一次进步</h3>
        <p className="muted" style={{ marginBottom: 0 }}>清楚的目标、适度的留白，以及及时的回应，让每一份认真都被看见。</p></div>
    </ConsoleShell>
  );
}
