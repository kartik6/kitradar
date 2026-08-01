package com.kitradar.normalize;

import com.kitradar.domain.SizeNorm;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SizeNormalizer {

    private static final Map<String, SizeNorm> SIZES = Map.ofEntries(
            Map.entry("XS", SizeNorm.XS),
            Map.entry("S", SizeNorm.S),
            Map.entry("M", SizeNorm.M),
            Map.entry("L", SizeNorm.L),
            Map.entry("XL", SizeNorm.XL),
            Map.entry("XXL", SizeNorm.XXL),
            Map.entry("2XL", SizeNorm.XXL),
            Map.entry("XXXL", SizeNorm.XXXL),
            Map.entry("3XL", SizeNorm.XXXL),
            Map.entry("XXXXL", SizeNorm.XXXXL),
            Map.entry("4XL", SizeNorm.XXXXL));

    public Optional<SizeNorm> normalize(String sizeLabel) {
        if (sizeLabel == null || sizeLabel.isBlank()) {
            return Optional.empty();
        }

        String normalized = sizeLabel.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\s._-]+", "");
        return Optional.ofNullable(SIZES.get(normalized));
    }
}
