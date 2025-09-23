package org.carl.app.command;

import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.client.dto.cmd.PointUpsertCmd;
import org.carl.component.dto.EntityResponse;
import org.carl.domain.gateway.IQdrantGateway;
import org.carl.infrastructure.common.exception.QdrantException;
import org.carl.utils.Assert;

@ApplicationScoped
public class PointUpsertCmdExe {
    @Inject IQdrantGateway qdrantGateway;

    public Uni<EntityResponse> execute(PointUpsertCmd pointUpsertCmd) {
        Assert.assertNotEmpty(pointUpsertCmd.getCollectionName(), "collectionName can not be null");
        Assert.assertNotEmpty(pointUpsertCmd.getData(), "points can not be null");
        return qdrantGateway
                .pointUpsert(pointUpsertCmd)
                .onItem()
                .transform(response -> EntityResponse.buildSuccess())
                .onFailure()
                .transform(QdrantException::qdrantUpsertException);
    }
}
