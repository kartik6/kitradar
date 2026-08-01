package com.kitradar.normalize.version;

import com.kitradar.domain.KitVersion;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class CategoryVersionRule implements VersionRule {

    private static final Map<String, KitVersion> CATEGORY_VERSIONS = Map.ofEntries(
            Map.entry("FAN VERSION", KitVersion.FAN),
            Map.entry("PLAYER VERSION", KitVersion.PLAYER),
            Map.entry("MASTER", KitVersion.MASTER),
            Map.entry("MASTER VERSION", KitVersion.MASTER),
            Map.entry("RETRO", KitVersion.RETRO),
            Map.entry("RETRO VERSION", KitVersion.RETRO),
            Map.entry("KIDS", KitVersion.KIDS),
            Map.entry("WOMEN", KitVersion.WOMEN));

    @Override
    public Optional<KitVersion> resolve(VersionCandidate candidate) {
        if (candidate.category() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(CATEGORY_VERSIONS.get(candidate.category().trim().toUpperCase(Locale.ROOT)));
    }
}
