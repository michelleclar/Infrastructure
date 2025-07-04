package org.carl.domain.gateway;

import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import io.smallrye.mutiny.Uni;

import org.carl.client.dto.cmd.PointUpsertCmd;
import org.carl.client.dto.query.RecommendationQ;
import org.carl.domain.qdrant.PointsQuery;
import org.carl.domain.qdrant.VectorCollection;

import java.util.List;

public interface IQdrantGateway {
    Uni<Integer> pointUpsert(String collection, List<Points.PointStruct> points);

    Uni<List<Points.ScoredPoint>> pointsQuery(String collection, PointsQuery q);

    Uni<Boolean> collectionUpsert(VectorCollection collection);

    Uni<Boolean> collectionUpsert(String collectionName, int size, Collections.Distance distance);

    Uni<Integer> pointUpsert(PointUpsertCmd pointUpsertCmd);

    Uni<List<Points.ScoredPoint>> recommend(RecommendationQ q);
}
