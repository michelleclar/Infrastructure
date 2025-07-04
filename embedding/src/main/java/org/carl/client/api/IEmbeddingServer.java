package org.carl.client.api;

import io.smallrye.mutiny.Uni;

import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.client.dto.cmd.CollectionCreateCmd;
import org.carl.client.dto.cmd.PointUpsertCmd;
import org.carl.client.dto.query.DictSearchQ;
import org.carl.client.dto.query.PointsQ;
import org.carl.component.dto.EntityResponse;
import org.carl.component.dto.MultiEntityResponse;

// NOTE: embedding server interface
public interface IEmbeddingServer {
    Uni<MultiEntityResponse<ScoredPointCO>> dictSearch(DictSearchQ dictSearchQ);

    Uni<MultiEntityResponse<ScoredPointCO>> pointQuery(PointsQ pointsQ);

    Uni<EntityResponse> collectionCreate(CollectionCreateCmd collectionCreateCmd);

    Uni<EntityResponse> pointUpsert(PointUpsertCmd pointUpsertCmd);
}
