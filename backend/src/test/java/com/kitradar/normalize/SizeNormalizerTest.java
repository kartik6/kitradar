package com.kitradar.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import com.kitradar.domain.SizeNorm;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SizeNormalizerTest {

    private final SizeNormalizer normalizer = new SizeNormalizer();

    @ParameterizedTest
    @MethodSource("sizes")
    void normalizesAdultSizes(String raw, Optional<SizeNorm> expected) {
        assertThat(normalizer.normalize(raw)).isEqualTo(expected);
    }

    private static Stream<Arguments> sizes() {
        return Stream.of(
                Arguments.of("S", Optional.of(SizeNorm.S)),
                Arguments.of(" xl ", Optional.of(SizeNorm.XL)),
                Arguments.of("2XL", Optional.of(SizeNorm.XXL)),
                Arguments.of("3XL", Optional.of(SizeNorm.XXXL)),
                Arguments.of("XXXXL", Optional.of(SizeNorm.XXXXL)),
                Arguments.of("4-xl", Optional.of(SizeNorm.XXXXL)),
                Arguments.of("Messi 10", Optional.empty()),
                Arguments.of("Default Title", Optional.empty()),
                Arguments.of("6-7Y", Optional.empty()));
    }
}
