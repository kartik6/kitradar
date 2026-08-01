import { FavoritesView } from "@/components/FavoritesView";

export default function Favorites() {
  return <><section className="mb-7"><p className="mb-2 text-xs font-bold uppercase tracking-[0.22em] text-signal">Your shortlist</p><h1 className="text-3xl font-black tracking-tight text-cream sm:text-5xl">Saved kits.</h1><p className="mt-3 text-sm text-zinc-400">Stored only in this browser — no account required.</p></section><FavoritesView /></>;
}
