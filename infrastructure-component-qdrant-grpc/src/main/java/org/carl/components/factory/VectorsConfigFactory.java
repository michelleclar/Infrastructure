package org.carl.components.factory;

import io.qdrant.client.grpc.Collections;

public final class VectorsConfigFactory {
    public static Collections.VectorsConfig build(long size, Collections.Distance distance) {
        return Collections.VectorsConfig.newBuilder()
                .setParams(
                        Collections.VectorParams.newBuilder().setSize(size).setDistance(distance))
                .build();
    }
}
