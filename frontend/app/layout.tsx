import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "KitRadar — Football kit finder",
  description: "Compare football jersey listings across Indian stores.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>
        <header className="sticky top-0 z-20 border-b border-line/90 bg-ink/95 backdrop-blur">
          <nav className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6">
            <Link href="/" className="text-xl font-black tracking-tight text-white">KIT<span className="text-signal">RADAR</span></Link>
            <div className="flex items-center gap-4 text-sm font-medium text-zinc-300">
              <Link href="/clearance" className="hover:text-signal">Clearance</Link>
              <Link href="/favorites" className="hover:text-signal">Favourites</Link>
            </div>
          </nav>
        </header>
        <main className="mx-auto min-h-[calc(100vh-160px)] max-w-7xl px-4 py-7 sm:px-6">{children}</main>
        <footer className="border-t border-line px-4 py-7 text-center text-xs leading-5 text-zinc-500 sm:px-6">
          Not affiliated with any store. Prices and availability are indicative — confirm on the seller&apos;s site.
        </footer>
      </body>
    </html>
  );
}
