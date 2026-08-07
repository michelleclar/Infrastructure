package org.carl.infra.discover.consul;

final class ConsulIndex {

    private ConsulIndex() {}

    static long next(long requested, long returned) {
        if (returned <= 0) {
            return 1;
        }
        if (requested > 0 && returned < requested) {
            return 0;
        }
        return returned;
    }
}
