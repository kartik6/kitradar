package com.kitradar.search;

import com.kitradar.domain.KitVersion;
import com.kitradar.domain.SizeNorm;
import java.math.BigDecimal;
import java.util.List;

public record KitQuery(
        List<Long> ids,
        String text,
        List<String> stores,
        List<SizeNorm> sizes,
        List<KitVersion> versions,
        boolean inStockOnly,
        boolean onClearance,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean includeOtherKinds,
        SortOption sort,
        int page,
        int pageSize) {

    public KitQuery {
        ids = ids == null ? List.of() : List.copyOf(ids);
        stores = stores == null ? List.of() : List.copyOf(stores);
        sizes = sizes == null ? List.of() : List.copyOf(sizes);
        versions = versions == null ? List.of() : List.copyOf(versions);
        sort = sort == null ? SortOption.FEATURED : sort;
    }
}
