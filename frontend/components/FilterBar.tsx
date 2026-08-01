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
        <select value={params.get("sort") ?? "latest_added"} onChange={(event) => update("sort", [event.target.value])} className="rounded-xl border border-line bg-[#181221] px-3 py-2.5 text-sm">
          <option value="latest_added">Latest arrivals</option><option value="featured">Mixed picks</option><option value="price_asc">Price: low to high</option><option value="price_desc">Price: high to low</option><option value="discount">Biggest discount</option><option value="newest">Newest at seller</option>
        </select>
        <label className="flex items-center gap-2 rounded-xl border border-line bg-[#181221] px-3 py-2.5 text-sm"><input type="checkbox" checked={params.get("inStock") === "true"} onChange={(event) => update("inStock", event.target.checked ? ["true"] : [])} /> In stock</label>
        <button type="button" onClick={() => router.replace(pathname)} className="rounded-xl border border-line px-3 py-2.5 text-sm text-zinc-400 transition-colors hover:border-signal/70 hover:text-cream">Clear</button>
      </div>
      <div className="mt-4 grid gap-4 border-t border-line pt-4 md:grid-cols-3">
        <ChecklistSelector label="Size" name="size" options={sizes.map((size) => ({ value: size, label: size }))} selected={params.getAll("size")} onToggle={toggle} />
        <ChecklistSelector label="Version" name="version" options={versions.map((version) => ({ value: version, label: version.replaceAll("_", " ") }))} selected={params.getAll("version")} onToggle={toggle} />
        <ChecklistSelector label="Store" name="store" options={stores.map((store) => ({ value: store.slug, label: store.name }))} selected={params.getAll("store")} onToggle={toggle} />
      </div>
    </section>
  );
}

type ChecklistOption = { value: string; label: string };

function ChecklistSelector({
  label,
  name,
  options,
  selected,
  onToggle,
}: {
  label: string;
  name: string;
  options: ChecklistOption[];
  selected: string[];
  onToggle(name: string, value: string): void;
}) {
  const selectedCount = selected.length;

  return (
    <div>
      <p className="mb-2 text-xs font-bold uppercase tracking-wider text-zinc-500">{label}</p>
      <details className="group relative">
        <summary className="flex cursor-pointer list-none items-center justify-between rounded-xl border border-line bg-[#181221] px-3 py-2.5 text-sm text-zinc-300 transition hover:border-signal/60 hover:text-cream [&::-webkit-details-marker]:hidden">
          <span>{selectedCount ? `${selectedCount} selected` : `Select ${label.toLowerCase()}`}</span>
          <span className="ml-3 text-signal transition-transform group-open:rotate-180" aria-hidden="true">⌄</span>
        </summary>
        <div className="absolute z-10 mt-2 max-h-64 w-full overflow-y-auto rounded-xl border border-line bg-[#181221] p-2 shadow-2xl shadow-black/40">
          {options.map((option) => {
            const checked = selected.includes(option.value);
            return (
              <label key={option.value} className="flex cursor-pointer items-center gap-2 rounded-lg px-2.5 py-2 text-sm text-zinc-300 transition hover:bg-white/5 hover:text-cream">
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={() => onToggle(name, option.value)}
                  className="h-4 w-4 rounded border-line accent-signal"
                />
                <span>{option.label}</span>
              </label>
            );
          })}
        </div>
      </details>
    </div>
  );
}
