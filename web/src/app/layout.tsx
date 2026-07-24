import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Seedie Console",
  description: "Agency and teacher console for Seedie",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  );
}
