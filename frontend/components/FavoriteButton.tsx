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
    <button type="button" onClick={toggle} aria-label={saved ? "Remove from favourites" : "Save to favourites"}
      className="absolute right-2 top-2 grid h-9 w-9 place-items-center rounded-full bg-ink/75 text-lg text-cream backdrop-blur transition hover:bg-signal hover:text-ink">
      {saved ? "♥" : "♡"}
    </button>
  );
}

export { key as favoriteStorageKey };
