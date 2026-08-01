import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "KitPaglu — Football kit finder",
  description: "Compare football jersey listings across Indian stores.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>
        <header className="sticky top-0 z-20 border-b border-line/90 bg-ink/85 backdrop-blur-xl">
          <nav className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6">
            <Link href="/" className="flex items-center gap-2.5 text-xl font-black tracking-tight text-cream" aria-label="KitPaglu home">
              <span className="grid h-8 w-8 -rotate-3 place-items-center rounded-lg bg-signal text-xs font-black tracking-tighter text-ink shadow-[3px_3px_0_#b84e62]">KP</span>
              <span>kit<span className="text-signal">paglu</span></span>
            </Link>
            <div className="flex items-center gap-4 text-sm font-medium text-zinc-300">
              <Link href="/clearance" className="transition-colors hover:text-signal">Clearance</Link>
              <Link href="/favorites" className="transition-colors hover:text-signal">Favourites</Link>
            </div>
          </nav>
        </header>
        <main className="mx-auto min-h-[calc(100vh-160px)] max-w-7xl px-4 py-7 sm:px-6">{children}</main>
        <footer className="border-t border-line/80 px-4 py-7 text-center text-xs leading-5 text-zinc-500 sm:px-6">
          Not affiliated with any store. Prices and availability are indicative — confirm on the seller&apos;s site. Prices may not include shipping charges.
        </footer>
      </body>
    </html>
  );
}
