import { Catalogue, toParams } from "@/app/page";
import { getKits, getStores } from "@/lib/api";

export const dynamic = "force-dynamic";

type SearchParams = Record<string, string | string[] | undefined>;

export default async function Clearance({ searchParams }: { searchParams: Promise<SearchParams> }) {
  const params = toParams(await searchParams);
  params.set("onClearance", "true");
  if (!params.has("sort")) params.set("sort", "discount");
  const [page, stores] = await Promise.all([getKits(params), getStores()]);
  return <Catalogue title="Clearance radar." intro="Genuine seller-listed discounts, ordered by biggest saving." page={page} stores={stores} query={params.toString()} />;
}
