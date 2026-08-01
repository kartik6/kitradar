"use client";

import { Store } from "@/lib/api";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState, useTransition } from "react";

const sizes = ["S", "M", "L", "XL", "XXL", "XXXL"];
const versions = ["FAN", "PLAYER", "MASTER", "RETRO", "KIDS", "WOMEN", "LONG_SLEEVE"];

export function FilterBar({ stores }: { stores: Store[] }) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const [pending, startTransition] = useTransition();
  const [text, setText] = useState(params.get("q") ?? "");

  useEffect(() => setText(params.get("q") ?? ""), [params]);
  useEffect(() => {
    const timer = window.setTimeout(() => update("q", text ? [text] : []), 300);
    return () => window.clearTimeout(timer);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [text]);

  function update(name: string, values: string[]) {
    const next = new URLSearchParams(params.toString());
    next.delete(name);
    values.forEach((value) => next.append(name, value));
    next.delete("page");
    startTransition(() => router.replace(`${pathname}?${next.toString()}`, { scroll: false }));
  }

  function toggle(name: string, value: string) {
    const selected = params.getAll(name);
    update(name, selected.includes(value) ? selected.filter((entry) => entry !== value) : [...selected, value]);
  }

  return (
    <section aria-label="Filters" className={`mb-7 rounded-2xl border border-line bg-panel p-4 ${pending ? "opacity-70" : ""}`}>
      <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_auto_auto_auto]">
        <input value={text} onChange={(event) => setText(event.target.value)} placeholder="Search clubs, teams, kits…" aria-label="Search kits"
          className="w-full rounded-xl border border-line bg-[#181221] px-3 py-2.5 text-sm placeholder:text-zinc-600 focus:border-signal" />
        <select value={params.get("sort") ?? "featured"} onChange={(event) => update("sort", [event.target.value])} className="rounded-xl border border-line bg-[#181221] px-3 py-2.5 text-sm">
          <option value="featured">Mixed picks</option><option value="price_asc">Price: low to high</option><option value="price_desc">Price: high to low</option><option value="discount">Biggest discount</option><option value="newest">Newest</option>
        </select>
        <label className="flex items-center gap-2 rounded-xl border border-line bg-[#181221] px-3 py-2.5 text-sm"><input type="checkbox" checked={params.get("inStock") === "true"} onChange={(event) => update("inStock", event.target.checked ? ["true"] : [])} /> In stock</label>
        <button type="button" onClick={() => router.replace(pathname)} className="rounded-xl border border-line px-3 py-2.5 text-sm text-zinc-400 transition-colors hover:border-signal/70 hover:text-cream">Clear</button>
      </div>
      <div className="mt-4 grid gap-4 border-t border-line pt-4 md:grid-cols-3">
        <FilterGroup label="Size">{sizes.map((size) => <Pill key={size} active={params.getAll("size").includes(size)} onClick={() => toggle("size", size)}>{size}</Pill>)}</FilterGroup>
        <FilterGroup label="Version">{versions.map((version) => <Pill key={version} active={params.getAll("version").includes(version)} onClick={() => toggle("version", version)}>{version.replaceAll("_", " ")}</Pill>)}</FilterGroup>
        <FilterGroup label="Store">{stores.map((store) => <Pill key={store.slug} active={params.getAll("store").includes(store.slug)} onClick={() => toggle("store", store.slug)}>{store.name}</Pill>)}</FilterGroup>
      </div>
    </section>
  );
}

function FilterGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return <div><p className="mb-2 text-xs font-bold uppercase tracking-wider text-zinc-500">{label}</p><div className="flex flex-wrap gap-1.5">{children}</div></div>;
}

function Pill({ active, children, onClick }: { active: boolean; children: React.ReactNode; onClick(): void }) {
  return <button type="button" onClick={onClick} className={`rounded-full border px-2.5 py-1 text-xs font-medium transition ${active ? "border-signal bg-signal text-ink" : "border-line text-zinc-400 hover:border-signal/60 hover:text-cream"}`}>{children}</button>;
}
