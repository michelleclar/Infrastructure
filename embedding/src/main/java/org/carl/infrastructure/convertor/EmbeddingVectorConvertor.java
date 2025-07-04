package org.carl.infrastructure.convertor;

import io.embeddingj.client.grpc.EmbeddingOuterClass;

import org.carl.domain.embedding.TextVector;

public class EmbeddingVectorConvertor {
    public static TextVector toTextEntity(
            EmbeddingOuterClass.TextVectorResponse textVectorResponse) {
        TextVector textVector = new TextVector();
        textVector.setVector(textVectorResponse.getVectorList());
        textVector.setSize(textVectorResponse.getVectorList().size());
        return textVector;
    }
}
