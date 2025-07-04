package org.carl.domain.gateway;

import io.embeddingj.client.grpc.EmbeddingOuterClass;
import io.smallrye.mutiny.Uni;

/**
 * embedding gateway
 *
 * <p>include: qdrant grpc client and embedding grpc client
 */
public interface IEmbeddingGateway {
    Uni<EmbeddingOuterClass.TextVectorResponse> textToVector(String text);
}
