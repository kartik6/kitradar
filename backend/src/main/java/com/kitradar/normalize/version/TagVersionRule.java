package com.kitradar.normalize.version;

import com.kitradar.domain.KitVersion;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class TagVersionRule implements VersionRule {

    private static final Map<String, KitVersion> TAG_VERSIONS = Map.ofEntries(
            Map.entry("player version", KitVersion.PLAYER),
            Map.entry("players version", KitVersion.PLAYER),
            Map.entry("master", KitVersion.MASTER),
            Map.entry("stadium kit", KitVersion.FAN),
            Map.entry("retro", KitVersion.RETRO),
            Map.entry("kids", KitVersion.KIDS),
            Map.entry("youth", KitVersion.KIDS),
            Map.entry("women", KitVersion.WOMEN));

    @Override
    public Optional<KitVersion> resolve(VersionCandidate candidate) {
        return candidate.tags().stream()
                .filter(tag -> tag != null)
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .map(TAG_VERSIONS::get)
                .filter(version -> version != null)
                .findFirst();
    }
}
