import Image from "next/image";
import { KitCard as KitCardData } from "@/lib/api";
import { currency } from "@/lib/format";
import { FavoriteButton } from "./FavoriteButton";

export function KitCard({ kit }: { kit: KitCardData }) {
  return (
    <article className="group relative overflow-hidden rounded-2xl border border-line bg-panel/95 shadow-[0_14px_30px_rgb(9_5_14_/_18%)] transition hover:-translate-y-0.5 hover:border-signal/70">
      <FavoriteButton kitId={kit.id} />
      <a href={kit.productUrl} target="_blank" rel="noopener nofollow sponsored" className="block">
        <div className="relative aspect-square bg-[#17111f]">
          {kit.imageUrl ? <Image src={kit.imageUrl} alt={kit.title} fill sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 20vw" className="object-cover transition duration-300 group-hover:scale-[1.025]" />
            : <div className="grid h-full place-items-center text-sm text-zinc-600">No image</div>}
          {!kit.inStock && <span className="absolute bottom-2 left-2 rounded bg-ink/90 px-2 py-1 text-xs font-semibold text-zinc-200">Out of stock</span>}
        </div>
        <div className="space-y-3 p-3.5">
          <div className="flex gap-2 text-[10px] font-bold tracking-wide">
            <span className="rounded bg-[#342742] px-2 py-1 text-zinc-300">{kit.storeName}</span>
            {kit.version !== "UNKNOWN" && <span className="rounded bg-signal px-2 py-1 text-ink">{kit.version.replaceAll("_", " ")}</span>}
          </div>
          <h2 className="line-clamp-2 min-h-10 text-sm font-semibold leading-5 text-cream">{kit.title}</h2>
          <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1">
            <span className="text-lg font-bold text-cream">{currency.format(kit.sellingPrice)}</span>
            {kit.mrp && <><span className="text-xs text-zinc-500 line-through">MRP {currency.format(kit.mrp)}</span><span className="text-xs font-bold text-signal">{kit.discountPercent}% off</span></>}
          </div>
          {kit.sizes.length > 0 && <p className="truncate text-xs text-zinc-500">Sizes: {kit.sizes.filter((size) => size.available).map((size) => size.label).join(", ") || "none available"}</p>}
        </div>
      </a>
    </article>
  );
}
