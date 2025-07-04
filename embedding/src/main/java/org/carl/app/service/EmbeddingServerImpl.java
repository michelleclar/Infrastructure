package org.carl.app.service;

import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.app.command.CollectionCreateCmdExe;
import org.carl.app.command.PointUpsertCmdExe;
import org.carl.app.query.DictSearchQExe;
import org.carl.app.query.PointsQExe;
import org.carl.client.api.IEmbeddingServer;
import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.client.dto.cmd.CollectionCreateCmd;
import org.carl.client.dto.cmd.PointUpsertCmd;
import org.carl.client.dto.query.DictSearchQ;
import org.carl.client.dto.query.PointsQ;
import org.carl.component.dto.EntityResponse;
import org.carl.component.dto.MultiEntityResponse;

@ApplicationScoped
public class EmbeddingServerImpl implements IEmbeddingServer {
    @Inject DictSearchQExe dictSearchQExe;
    @Inject PointUpsertCmdExe pointUpsertCmdExe;
    @Inject PointsQExe pointsQExe;
    @Inject CollectionCreateCmdExe collectionCreateCmdExe;

    @Override
    public Uni<MultiEntityResponse<ScoredPointCO>> dictSearch(DictSearchQ dictSearchQ) {
        return dictSearchQExe.executer(dictSearchQ);
    }

    @Override
    public Uni<MultiEntityResponse<ScoredPointCO>> pointQuery(PointsQ pointsQ) {
        return pointsQExe.executer(pointsQ);
    }

    @Override
    public Uni<EntityResponse> collectionCreate(CollectionCreateCmd collectionCreateCmd) {
        return collectionCreateCmdExe.execute(collectionCreateCmd);
    }

    @Override
    public Uni<EntityResponse> pointUpsert(PointUpsertCmd pointUpsertCmd) {
        return pointUpsertCmdExe.execute(pointUpsertCmd);
    }
}
