export const currency = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

export function relativeTime(value: string | null) {
  if (!value) return "Not refreshed yet";
  const hours = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 3_600_000));
  return hours === 0 ? "Updated just now" : `Updated ${hours}h ago`;
}
