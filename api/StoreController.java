package com.kitradar.api;

import com.kitradar.api.dto.StoreDto;
import com.kitradar.search.MetaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final MetaService metaService;

    public StoreController(MetaService metaService) {
        this.metaService = metaService;
    }

    @GetMapping
    public List<StoreDto> stores() {
        return metaService.stores();
    }
}
