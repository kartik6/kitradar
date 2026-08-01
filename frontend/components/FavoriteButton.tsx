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
  const [popping, setPopping] = useState(false);

  useEffect(() => setSaved(readFavorites().includes(kitId)), [kitId]);

  function toggle() {
    const favorites = readFavorites();
    const next = favorites.includes(kitId)
      ? favorites.filter((id) => id !== kitId)
      : [kitId, ...favorites].slice(0, limit);
    const isSaved = next.includes(kitId);
    localStorage.setItem(key, JSON.stringify(next));
    setSaved(isSaved);
    setPopping(false);
    if (isSaved) window.setTimeout(() => setPopping(true), 0);
    window.dispatchEvent(new Event("kitpaglu:favorites"));
  }

  return (
    <button type="button" onClick={toggle} aria-pressed={saved} aria-label={saved ? "Remove from favourites" : "Save to favourites"} title={saved ? "Remove from favourites" : "Save to favourites"}
      className={`favorite-button absolute right-3 top-3 z-10 inline-grid h-10 w-10 place-items-center rounded-full border text-xl font-bold shadow-lg backdrop-blur transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-signal ${saved ? "border-signal bg-signal text-ink" : "border-signal/80 bg-[#21172d]/95 text-cream hover:border-signal hover:bg-[#342742]"} ${popping ? "favorite-button--popping" : ""}`}>
      <span aria-hidden="true" onAnimationEnd={() => setPopping(false)} className={`favorite-heart leading-none ${popping ? "favorite-heart--popping" : ""}`}>{saved ? "♥" : "♡"}</span>
    </button>
  );
}

export { key as favoriteStorageKey };
