package com.kitradar.normalize.version;

import com.kitradar.domain.KitVersion;
import java.util.Optional;

public interface VersionRule {

    Optional<KitVersion> resolve(VersionCandidate candidate);
}
