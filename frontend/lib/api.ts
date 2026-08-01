export type KitVersion = "FAN" | "PLAYER" | "MASTER" | "RETRO" | "KIDS" | "WOMEN" | "LONG_SLEEVE" | "UNKNOWN";

export type KitSize = { label: string; available: boolean };

export type KitCard = {
  id: number;
  title: string;
  storeSlug: string;
  storeName: string;
  imageUrl: string | null;
  productUrl: string;
  version: KitVersion;
  sellingPrice: number;
  mrp: number | null;
  discountPercent: number;
  inStock: boolean;
  sizes: KitSize[];
};

export type KitPage = {
  content: KitCard[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  lastUpdatedAt: string | null;
};

export type Store = { slug: string; name: string; productCount: number };

const apiBase = process.env.KITRADAR_API_URL ?? "http://localhost:8080";

export function clientApiBase() {
  return process.env.NEXT_PUBLIC_KITRADAR_API_URL ?? apiBase;
}

export async function getKits(params: URLSearchParams): Promise<KitPage> {
  const response = await fetch(`${apiBase}/api/v1/kits?${params.toString()}`, {
    next: { revalidate: 300 },
  });
  if (!response.ok) throw new Error(`Catalogue request failed (${response.status})`);
  return response.json();
}

export async function getStores(): Promise<Store[]> {
  const response = await fetch(`${apiBase}/api/v1/stores`, { next: { revalidate: 300 } });
  if (!response.ok) throw new Error(`Store request failed (${response.status})`);
  return response.json();
}
