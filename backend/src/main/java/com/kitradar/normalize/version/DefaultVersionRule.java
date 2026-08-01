package com.kitradar.normalize.version;

import com.kitradar.domain.KitVersion;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class DefaultVersionRule implements VersionRule {

    @Override
    public Optional<KitVersion> resolve(VersionCandidate candidate) {
        return Optional.of(KitVersion.UNKNOWN);
    }
}
