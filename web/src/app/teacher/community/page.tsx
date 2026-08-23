import { requireTeacher } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import { teacherTabs } from "@/lib/teacher-assignments";
import { CreatePostForm } from "./CreatePostForm";
import { DeletePostButton, FeaturePostButton } from "./PostActions";

type PostRow = {
  id: string;
  author_id: string;
  author_role: string;
  author_display_name: string;
  title: string | null;
  body: string;
  class_id: string | null;
  author_grade: string | null;
  target_grades: string[] | null;
  is_featured: boolean;
  created_at: string;
};

export default async function TeacherCommunityPage() {
  const profile = await requireTeacher();
  const supabase = await createClient();

  const { data: posts, error: listError } = await supabase
    .from("community_posts")
    .select(
      "id, author_id, author_role, author_display_name, title, body, class_id, author_grade, target_grades, is_featured, created_at"
    )
    .order("created_at", { ascending: false })
    .limit(100);

  const list = (posts ?? []) as PostRow[];

  return (
    <ConsoleShell
      profile={profile}
      title="教师控制台"
      tabs={teacherTabs("community")}
    >
      <div className="card stack">
        <CreatePostForm />
      </div>

      <div className="card stack" style={{ marginTop: 16 }}>
        <h2 style={{ margin: 0 }}>可见帖子</h2>
        <p className="muted" style={{ margin: 0 }}>
          含本机构教师帖、以及名下学生的班级帖。可将名下学生帖设为精华，使同机构同年级可见。
        </p>
        {listError && (
          <p className="error">加载失败：{listError.message}</p>
        )}
        {!listError && list.length === 0 && (
          <p className="muted">还没有帖子。</p>
        )}
        {list.map((post) => {
          const authorLabel =
            post.author_display_name?.trim() || post.author_id.slice(0, 8);
          const roleLabel = post.author_role === "teacher" ? "教师" : "学生";
          const metaParts: string[] = [roleLabel];
          if (post.author_role === "teacher" && post.target_grades?.length) {
            metaParts.push(
              `可见：${
                Array.isArray(post.target_grades)
                  ? post.target_grades.join("、")
                  : String(post.target_grades)
              }`
            );
          }
          if (post.author_role === "student") {
            if (post.class_id) metaParts.push(`班级 ${post.class_id}`);
            if (post.author_grade) metaParts.push(post.author_grade);
            if (post.is_featured) metaParts.push("精华");
          }
          const canModerateStudent =
            post.author_role === "student" && post.author_id !== profile.id;
          const canDelete =
            post.author_id === profile.id || canModerateStudent;

          return (
            <article
              key={post.id}
              className="stack"
              style={{
                borderTop: "1px solid var(--line)",
                paddingTop: 12,
                gap: 8,
              }}
            >
              <div className="row" style={{ justifyContent: "space-between" }}>
                <strong>{post.title || "（无标题）"}</strong>
                <span className="muted" style={{ fontSize: 13 }}>
                  {new Date(post.created_at).toLocaleString()}
                </span>
              </div>
              <p className="muted" style={{ margin: 0, fontSize: 13 }}>
                {authorLabel} · {metaParts.join(" · ")}
              </p>
              <p style={{ margin: 0, whiteSpace: "pre-wrap" }}>{post.body}</p>
              <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
                {canModerateStudent && (
                  <FeaturePostButton
                    postId={post.id}
                    featured={post.is_featured}
                  />
                )}
                {canDelete && <DeletePostButton postId={post.id} />}
              </div>
            </article>
          );
        })}
      </div>
    </ConsoleShell>
  );
}
