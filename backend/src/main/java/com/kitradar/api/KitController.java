package com.kitradar.api;

import com.kitradar.api.dto.KitPageDto;
import com.kitradar.domain.KitVersion;
import com.kitradar.domain.SizeNorm;
import com.kitradar.search.KitQuery;
import com.kitradar.search.KitQueryService;
import com.kitradar.search.SortOption;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/kits")
public class KitController {

    private final KitQueryService queryService;

    public KitController(KitQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public KitPageDto search(
            @RequestParam(name = "id", required = false) List<Long> ids,
            @RequestParam(name = "q", required = false) String text,
            @RequestParam(name = "store", required = false) List<String> stores,
            @RequestParam(name = "size", required = false) List<SizeNorm> sizes,
            @RequestParam(name = "version", required = false) List<KitVersion> versions,
            @RequestParam(name = "inStock", defaultValue = "false") boolean inStockOnly,
            @RequestParam(name = "onClearance", defaultValue = "false") boolean onClearance,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "kind", defaultValue = "jersey") String kind,
            @RequestParam(name = "sort", defaultValue = "featured") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "24") int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and pageSize must be between 1 and 60");
        }
        boolean includeOtherKinds = switch (kind) {
            case "jersey" -> false;
            case "all" -> true;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kind must be jersey or all");
        };
        try {
            return queryService.search(new KitQuery(
                    ids, text, stores, sizes, versions, inStockOnly, onClearance, minPrice, maxPrice,
                    includeOtherKinds, SortOption.fromApiValue(sort), page, pageSize));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
