package org.carl.domain.qdrant;

import io.qdrant.client.grpc.Collections;

import org.carl.components.factory.VectorsConfigFactory;

public enum VectorCollection {
    face_vectors(VectorsConfigFactory.build(512, Collections.Distance.Cosine)),
    dict_vectors(VectorsConfigFactory.build(1024, Collections.Distance.Cosine)),
    resume_vectors(VectorsConfigFactory.build(1024, Collections.Distance.Cosine)),
    ;
    private final Collections.VectorsConfig vectorsConfig;

    VectorCollection(Collections.VectorsConfig vectorsConfig) {
        this.vectorsConfig = vectorsConfig;
    }

    public Collections.VectorsConfig getVectorsConfig() {
        return vectorsConfig;
    }
}
