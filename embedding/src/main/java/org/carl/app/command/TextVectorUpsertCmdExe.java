package org.carl.app.command;

import io.embeddingj.client.grpc.EmbeddingOuterClass;
import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.domain.gateway.IEmbeddingGateway;

import java.util.List;

@ApplicationScoped
public class TextVectorUpsertCmdExe {
    @Inject IEmbeddingGateway embeddingGateway;

    public Uni<List<Float>> execute(String text) {
        return embeddingGateway
                .textToVector(text)
                .onItem()
                .transform(EmbeddingOuterClass.TextVectorResponse::getVectorList);
    }
}
