package org.carl.app.query;

import io.qdrant.client.grpc.Points;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.client.dto.VectorQ;
import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.components.factory.WithVectorsSelectorFactory;
import org.carl.components.qdrant.QdrantGrpcClient;
import org.carl.infrastructure.component.web.config.exception.BizException;
import org.carl.infrastructure.component.web.config.exception.ExceptionReason;
import org.carl.infrastructure.convertor.ScoredPointConvertor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import static org.carl.components.factory.QueryFactory.nearest;
import static org.carl.components.factory.WithPayloadSelectorFactory.enable;

@ApplicationScoped
public class VectorQExe {
    @Inject QdrantGrpcClient qdrantGrpcClient;

    public Uni<List<ScoredPointCO>> executer(VectorQ q) {
        Objects.requireNonNull(q.getCollectionName(), "Collection name cannot be null");
        CompletionStage<Points.QueryResponse> completionStage =
                qdrantGrpcClient
                        .getPointsGrpcClient()
                        .query(
                                builder -> {
                                    builder.setCollectionName(q.getCollectionName());
                                    if (q.getFilter() != null) {
                                        builder.setFilter(q.getFilter());
                                    }
                                    if (q.getPointId() != null && q.getVector() != null) {
                                        throw new BizException(
                                                ExceptionReason.biz(
                                                        "id and vector don't togeter",
                                                        "grpc.qdrant",
                                                        "grpc.qdrant"));
                                    }
                                    if (q.getPointId() != null) {
                                        builder.setQuery(nearest(q.getPointId()));
                                    }
                                    if (q.getVector() != null) {
                                        builder.setQuery(nearest(q.getVector()));
                                    }
                                    if (q.getFilter() != null) {
                                        builder.setFilter(q.getFilter());
                                    }
                                    if (q.getLimit() != null) {
                                        builder.setLimit(q.getLimit());
                                    }
                                    builder.setWithPayload(enable(q.getWithPayload()));
                                    builder.setWithVectors(
                                            WithVectorsSelectorFactory.enable(q.getWithVector()));
                                    return builder.build();
                                })
                        .toCompletionStage();
        return Uni.createFrom()
                .completionStage(completionStage)
                .onItem()
                .transformToUni(
                        item -> {
                            List<Points.ScoredPoint> resultList = item.getResultList();
                            List<ScoredPointCO> r =
                                    resultList.stream()
                                            .map(ScoredPointConvertor::toClientObject)
                                            .toList();
                            return Uni.createFrom().item(r);
                        });
    }
}
