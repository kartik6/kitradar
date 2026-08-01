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
      className={`absolute right-2 top-2 inline-flex h-9 items-center gap-1.5 rounded-full px-3 text-xs font-bold backdrop-blur transition ${saved ? "bg-signal text-ink" : "bg-ink/85 text-cream hover:bg-[#342742]"}`}>
      <span aria-hidden="true" className="text-base leading-none">{saved ? "♥" : "♡"}</span>
      <span>{saved ? "Saved" : "Save"}</span>
    </button>
  );
}

export { key as favoriteStorageKey };
