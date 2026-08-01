package com.kitradar.normalize.version;

import java.util.List;

public record VersionCandidate(String title, List<String> tags, String category) {

    public VersionCandidate {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
