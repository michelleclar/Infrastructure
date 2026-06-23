package org.carl.infrastructure.mq.source;

import java.util.Objects;

public record SourcePackageLocation(Kind kind, String location) {
    public SourcePackageLocation {
        Objects.requireNonNull(kind, "kind must not be null");
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("location must not be blank");
        }
    }

    public static SourcePackageLocation file(String fileName) {
        return new SourcePackageLocation(Kind.FILE, fileName);
    }

    public static SourcePackageLocation url(String url) {
        return new SourcePackageLocation(Kind.URL, url);
    }

    public enum Kind {
        FILE,
        URL
    }
}
