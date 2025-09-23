package org.carl.adapter;

import io.smallrye.mutiny.Uni;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.carl.client.api.IRecommendServer;
import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.client.dto.query.RecommendationQ;
import org.carl.component.dto.MultiEntityResponse;
import org.carl.infrastructure.component.web.annotations.ControllerLogged;

@Path("/recommend")
@ControllerLogged
public class RecommendationController {
    @Inject IRecommendServer recommendServer;

    @POST
    public Uni<MultiEntityResponse<ScoredPointCO>> recommend(RecommendationQ q) {
        return recommendServer.recommend(q);
    }
}
