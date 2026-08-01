package com.kitradar.normalize.version;

import static org.assertj.core.api.Assertions.assertThat;

import com.kitradar.domain.KitVersion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class VersionResolverTest {

    private final VersionResolver resolver = new VersionResolver(List.of(
            new CategoryVersionRule(),
            new TagVersionRule(),
            new TitleVersionRule(Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC)),
            new DefaultVersionRule()));

    @Test
    void categoryWinsOverContradictoryTagsAndTitle() {
        KitVersion version = resolver.resolve(new VersionCandidate(
                "Barcelona Retro Master Fan Version",
                List.of("retro", "master"),
                "PLAYER VERSION"));

        assertThat(version).isEqualTo(KitVersion.PLAYER);
    }

    @Test
    void doesNotTreatSeasonNotationAsRetro() {
        assertThat(resolver.resolve(new VersionCandidate("Real Madrid Home 25/26", List.of(), null)))
                .isEqualTo(KitVersion.UNKNOWN);
    }

    @Test
    void recognizesOlderFourDigitYearsAsRetro() {
        assertThat(resolver.resolve(new VersionCandidate("Brazil Home 1998", List.of(), null)))
                .isEqualTo(KitVersion.RETRO);
    }

    @Test
    void treatsLongSleeveAsAWeakerSignalThanFanVersion() {
        assertThat(resolver.resolve(new VersionCandidate("Arsenal Fan Version Long Sleeve", List.of(), null)))
                .isEqualTo(KitVersion.FAN);
    }
}
