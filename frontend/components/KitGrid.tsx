"use client";

import { clientApiBase, KitCard as KitCardData, KitPage } from "@/lib/api";
import { useState } from "react";
import { KitCard } from "./KitCard";

export function KitGrid({ initialPage, query }: { initialPage: KitPage; query: string }) {
  const [extra, setExtra] = useState<KitCardData[]>([]);
  const [page, setPage] = useState(initialPage.page);
  const [loading, setLoading] = useState(false);
  const cards = [...initialPage.content, ...extra];
  const hasMore = page + 1 < initialPage.totalPages;

  async function loadMore() {
    setLoading(true);
    const params = new URLSearchParams(query);
    params.set("page", String(page + 1));
    try {
      const response = await fetch(`${clientApiBase()}/api/v1/kits?${params}`);
      if (!response.ok) throw new Error("Could not load more kits");
      const next: KitPage = await response.json();
      setExtra((items) => [...items, ...next.content]);
      setPage(next.page);
    } finally { setLoading(false); }
  }

  if (cards.length === 0) return <div className="rounded-2xl border border-dashed border-line p-12 text-center text-zinc-500">No kits match these filters.</div>;
  return <>
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">{cards.map((kit) => <KitCard key={kit.id} kit={kit} />)}</div>
    {hasMore && <div className="mt-8 text-center"><button type="button" disabled={loading} onClick={loadMore} className="rounded-xl bg-signal px-5 py-3 text-sm font-bold text-ink shadow-[3px_3px_0_#b84e62] transition hover:-translate-y-0.5 disabled:opacity-50">{loading ? "Loading…" : "Load more"}</button></div>}
  </>;
}
