package org.carl.app.query;

import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.client.dto.query.RecommendationQ;
import org.carl.component.dto.MultiEntityResponse;
import org.carl.domain.gateway.IQdrantGateway;
import org.carl.infrastructure.convertor.ScoredPointConvertor;
import org.carl.utils.Assert;

@ApplicationScoped
public class RecommendationQExe {
    @Inject IQdrantGateway qdrantGateway;

    /**
     * positive +
     *
     * <p>negative -
     *
     * @param q query
     * @return q
     */
    public Uni<MultiEntityResponse<ScoredPointCO>> executer(RecommendationQ q) {
        if (q.getPositiveVectorTexts() == null && q.getPositiveIds() == null) {
            Assert.assertTrue(true, "positive vectors and positive ids cannot be null");
        }
        return qdrantGateway
                .recommend(q)
                .onItem()
                .transform(item -> item.stream().map(ScoredPointConvertor::toClientObject).toList())
                .onItem()
                .transform(MultiEntityResponse::of);
    }
}
