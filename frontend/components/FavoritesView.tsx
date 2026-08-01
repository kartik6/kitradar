"use client";

import { clientApiBase, KitCard as KitCardData, KitPage } from "@/lib/api";
import { favoriteStorageKey } from "./FavoriteButton";
import { KitCard } from "./KitCard";
import { useEffect, useState } from "react";

function ids() {
  try {
    const saved = JSON.parse(localStorage.getItem(favoriteStorageKey) ?? "[]");
    return Array.isArray(saved) ? saved.filter(Number.isInteger) as number[] : [];
  } catch { return []; }
}

export function FavoritesView() {
  const [cards, setCards] = useState<KitCardData[] | null>(null);
  const [missing, setMissing] = useState(0);

  useEffect(() => {
    async function load() {
      const favoriteIds = ids();
      if (!favoriteIds.length) { setCards([]); return; }
      const pages = await Promise.all(Array.from({ length: Math.ceil(favoriteIds.length / 60) }, async (_, index): Promise<KitCardData[]> => {
        const params = new URLSearchParams({ page: "0", pageSize: "60" });
        favoriteIds.slice(index * 60, index * 60 + 60).forEach((id) => params.append("id", String(id)));
        const response = await fetch(`${clientApiBase()}/api/v1/kits?${params}`);
        if (!response.ok) return [];
        return (await response.json() as KitPage).content;
      }));
      const resolved = pages.flat();
      setCards(resolved);
      setMissing(favoriteIds.length - resolved.length);
    }
    load();
    window.addEventListener("kitradar:favorites", load);
    return () => window.removeEventListener("kitradar:favorites", load);
  }, []);

  if (cards === null) return <p className="text-zinc-500">Loading favourites…</p>;
  if (!cards.length) return <div className="rounded-2xl border border-dashed border-line p-12 text-center text-zinc-500">No saved kits yet. Tap the heart on any listing to keep it here.</div>;
  return <><div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">{cards.map((kit) => <KitCard key={kit.id} kit={kit} />)}</div>{missing > 0 && <p className="mt-6 text-sm text-zinc-500">{missing} saved {missing === 1 ? "kit is" : "kits are"} no longer listed.</p>}</>;
}
