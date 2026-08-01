package com.kitradar.normalize.version;

import com.kitradar.domain.KitVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class VersionResolver {

    private final List<VersionRule> rules;

    public VersionResolver(List<VersionRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public KitVersion resolve(VersionCandidate candidate) {
        return rules.stream()
                .map(rule -> rule.resolve(candidate))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(KitVersion.UNKNOWN);
    }
}
