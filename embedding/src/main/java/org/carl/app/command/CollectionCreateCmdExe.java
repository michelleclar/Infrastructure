package org.carl.app.command;

import io.qdrant.client.grpc.Collections;
import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.client.dto.cmd.CollectionCreateCmd;
import org.carl.component.dto.EntityResponse;
import org.carl.domain.gateway.IQdrantGateway;
import org.carl.utils.Assert;

@ApplicationScoped
public class CollectionCreateCmdExe {
    @Inject IQdrantGateway qdrantGateway;

    public Uni<EntityResponse> execute(CollectionCreateCmd cmd) {
        Assert.assertNotEmpty(cmd.getCollectionName(), "Collection name is empty");
        Assert.assertNotNull(cmd.getSize(), "Collection size is empty");
        if (cmd.getDistance() == null) {
            cmd.setDistance(Collections.Distance.Cosine);
        }
        return qdrantGateway
                .collectionUpsert(cmd.getCollectionName(), cmd.getSize(), cmd.getDistance())
                .onItem()
                .transform(
                        item -> {
                            if (item) {
                                return EntityResponse.buildSuccess();
                            }
                            return EntityResponse.buildFailure(
                                    "embedding.collection.create", "collection exits");
                        });
    }
}
