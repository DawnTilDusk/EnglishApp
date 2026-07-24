import { Suspense } from "react";
import LoginClient from "./LoginClient";

export default function Page() {
  return (
    <Suspense fallback={<main><p>加载中…</p></main>}>
      <LoginClient />
    </Suspense>
  );
}
