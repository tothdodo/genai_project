"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import "./globals.css"

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const pathname = usePathname()

  const linkClass = (href: string) =>
    cn(
      "rounded-md px-3 py-2 text-sm font-medium transition-colors",
      pathname === href
        ? "bg-primary text-primary-foreground"
        : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
    )

  return (
    <html lang="en">
      <body className="min-h-screen bg-background">
        <header className="border-b">
          <nav className="mx-auto flex max-w-4xl gap-2 p-4">
            <Link href="/home" className={linkClass("/home")}>
              Home
            </Link>
            <Link href="/upload" className={linkClass("/upload")}>
              Upload
            </Link>
          </nav>
        </header>

        <main className="mx-auto max-w-4xl p-6">
          {children}
        </main>
      </body>
    </html>
  )
}