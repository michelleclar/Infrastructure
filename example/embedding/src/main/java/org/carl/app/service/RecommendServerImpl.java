package org.carl.app.service;

import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.app.query.RecommendationQExe;
import org.carl.client.api.IRecommendServer;
import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.client.dto.query.RecommendationQ;
import org.carl.component.dto.MultiEntityResponse;

@ApplicationScoped
public class RecommendServerImpl implements IRecommendServer {
    @Inject RecommendationQExe recommendationQExe;

    @Override
    public Uni<MultiEntityResponse<ScoredPointCO>> recommend(RecommendationQ q) {
        return recommendationQExe.executer(q);
    }
}
