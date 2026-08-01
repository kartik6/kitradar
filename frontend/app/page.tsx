import { FilterBar } from "@/components/FilterBar";
import { KitGrid } from "@/components/KitGrid";
import { getKits, getStores } from "@/lib/api";
import { relativeTime } from "@/lib/format";

export const dynamic = "force-dynamic";

type SearchParams = Record<string, string | string[] | undefined>;

export default async function Home({ searchParams }: { searchParams: Promise<SearchParams> }) {
  const params = toParams(await searchParams);
  const [page, stores] = await Promise.all([getKits(params), getStores()]);
  return <Catalogue title="The kit hunt, made joyful." intro="Search live listings across the stores we track." page={page} stores={stores} query={params.toString()} />;
}

export function toParams(values: SearchParams) {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(values)) {
    if (Array.isArray(value)) value.forEach((entry) => params.append(key, entry));
    else if (value) params.set(key, value);
  }
  if (!params.has("page")) params.set("page", "0");
  if (!params.has("pageSize")) params.set("pageSize", "24");
  return params;
}

export function Catalogue({ title, intro, page, stores, query }: { title: string; intro: string; page: Awaited<ReturnType<typeof getKits>>; stores: Awaited<ReturnType<typeof getStores>>; query: string }) {
  return <>
    <section className="mb-7 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div><p className="mb-2 text-xs font-bold uppercase tracking-[0.22em] text-signal">KitPaglu picks</p><h1 className="text-3xl font-black tracking-tight text-cream sm:text-5xl">{title}</h1><p className="mt-3 max-w-xl text-sm text-zinc-400">{intro}</p></div>
      <p className="text-sm text-zinc-500"><span className="font-semibold text-zinc-200">{page.totalElements.toLocaleString()} kits found</span><br />{relativeTime(page.lastUpdatedAt)}</p>
    </section>
    <FilterBar stores={stores} />
    <KitGrid key={query} initialPage={page} query={query} />
  </>;
}
