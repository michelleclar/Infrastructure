package org.carl.client.api;

import io.smallrye.mutiny.Uni;

import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.client.dto.query.RecommendationQ;
import org.carl.component.dto.MultiEntityResponse;

public interface IRecommendServer {
    Uni<MultiEntityResponse<ScoredPointCO>> recommend(RecommendationQ q);
}
