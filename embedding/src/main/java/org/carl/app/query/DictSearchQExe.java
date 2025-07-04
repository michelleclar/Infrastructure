package org.carl.app.query;

import static org.carl.components.factory.ConditionFactory.match;
import static org.carl.components.factory.ConditionFactory.matchKeyword;

import io.qdrant.client.grpc.Points;
import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.app.command.TextVectorUpsertCmdExe;
import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.client.dto.query.DictSearchQ;
import org.carl.component.dto.MultiEntityResponse;
import org.carl.components.factory.QueryFactory;
import org.carl.domain.gateway.IQdrantGateway;
import org.carl.domain.qdrant.PointsQuery;
import org.carl.domain.qdrant.VectorCollection;
import org.carl.infrastructure.convertor.ScoredPointConvertor;
import org.carl.utils.Assert;

@ApplicationScoped
public class DictSearchQExe {
    @Inject TextVectorUpsertCmdExe textVectorUpsertCmdExe;
    @Inject IQdrantGateway qdrantGateway;

    public Uni<MultiEntityResponse<ScoredPointCO>> executer(DictSearchQ q) {
        Assert.assertNotNull(q.getKeyword(), "DictSearchQ must not be null");
        return textVectorUpsertCmdExe
                .execute(q.getKeyword())
                .onItem()
                .transformToUni(
                        item -> {
                            Points.Filter.Builder filter = Points.Filter.newBuilder();
                            if (q.getLevel() != null) {
                                filter.addMust(match("level", q.getLevel()));
                            }
                            filter.addMust(matchKeyword("dict_type", q.getDictType()));
                            PointsQuery pointsQuery =
                                    new PointsQuery()
                                            .setQuery(QueryFactory.nearest(item))
                                            .setFilter(filter.build())
                                            .setLimit(q.getLimit())
                                            .setWithVector(true)
                                            .setWithPayload(false);
                            return qdrantGateway
                                    .pointsQuery(VectorCollection.dict_vectors.name(), pointsQuery)
                                    .onItem()
                                    .transform(
                                            response ->
                                                    response.parallelStream()
                                                            .map(
                                                                    ScoredPointConvertor
                                                                            ::toClientObject)
                                                            .toList())
                                    .onItem()
                                    .transform(MultiEntityResponse::of);
                        });
    }
}
