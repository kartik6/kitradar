package com.kitradar.ingest.dotpe;

public record DotPeProductsResponse(DotPePageProps pageProps) {

    public record DotPePageProps(DotPeProductJson product) {
    }
}
