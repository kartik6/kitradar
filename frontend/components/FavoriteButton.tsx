"use client";

import { useEffect, useState } from "react";

const key = "kitpaglu:favorites:v1";
const limit = 100;

function readFavorites(): number[] {
  try {
    const value = JSON.parse(localStorage.getItem(key) ?? "[]");
    return Array.isArray(value) ? value.filter(Number.isInteger) : [];
  } catch { return []; }
}

export function FavoriteButton({ kitId }: { kitId: number }) {
  const [saved, setSaved] = useState(false);

  useEffect(() => setSaved(readFavorites().includes(kitId)), [kitId]);

  function toggle() {
    const favorites = readFavorites();
    const next = favorites.includes(kitId)
      ? favorites.filter((id) => id !== kitId)
      : [kitId, ...favorites].slice(0, limit);
    localStorage.setItem(key, JSON.stringify(next));
    setSaved(next.includes(kitId));
    window.dispatchEvent(new Event("kitpaglu:favorites"));
  }

  return (
    <button type="button" onClick={toggle} aria-pressed={saved} aria-label={saved ? "Remove from favourites" : "Save to favourites"}
      className={`absolute right-3 top-3 z-10 inline-flex h-10 items-center gap-1.5 rounded-full border px-3.5 text-xs font-extrabold shadow-lg backdrop-blur transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-signal ${saved ? "border-signal bg-signal text-ink" : "border-signal/80 bg-[#21172d]/95 text-cream hover:border-signal hover:bg-[#342742]"}`}>
      <span aria-hidden="true" className="text-base leading-none">{saved ? "♥" : "♡"}</span>
      <span>{saved ? "Favourited" : "Favourite"}</span>
    </button>
  );
}

export { key as favoriteStorageKey };
