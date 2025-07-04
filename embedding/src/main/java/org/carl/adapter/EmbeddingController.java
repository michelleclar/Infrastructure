package org.carl.adapter;

import io.smallrye.mutiny.Uni;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.carl.client.api.IEmbeddingServer;
import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.client.dto.cmd.CollectionCreateCmd;
import org.carl.client.dto.cmd.PointUpsertCmd;
import org.carl.client.dto.query.DictSearchQ;
import org.carl.client.dto.query.PointsQ;
import org.carl.component.dto.EntityResponse;
import org.carl.component.dto.MultiEntityResponse;
import org.carl.infrastructure.component.web.annotations.ControllerLogged;

@Path("/embedding")
@ControllerLogged
public class EmbeddingController {

    @Inject IEmbeddingServer embeddingServer;

    @POST
    @Path("/dict/search")
    public Uni<MultiEntityResponse<ScoredPointCO>> dictSearch(DictSearchQ dictSearchQ) {
        return embeddingServer.dictSearch(dictSearchQ);
    }

    @POST
    @Path("/point/upsert")
    public Uni<EntityResponse> pointUpsert(PointUpsertCmd pointUpsertCmd) {
        return embeddingServer.pointUpsert(pointUpsertCmd);
    }

    @POST
    @Path("/point/query")
    public Uni<MultiEntityResponse<ScoredPointCO>> pointQuery(PointsQ pointsQ) {
        return embeddingServer.pointQuery(pointsQ);
    }

    @POST
    @Path("/collection/create")
    public Uni<EntityResponse> collectionCreate(CollectionCreateCmd cmd) {
        return embeddingServer.collectionCreate(cmd);
    }
}
