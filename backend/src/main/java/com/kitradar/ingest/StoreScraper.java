package com.kitradar.ingest;

import com.kitradar.domain.Platform;
import com.kitradar.domain.Store;
import com.kitradar.ingest.model.RawProduct;
import java.util.List;

public interface StoreScraper {

    Platform platform();

    List<RawProduct> fetch(Store store);
}
