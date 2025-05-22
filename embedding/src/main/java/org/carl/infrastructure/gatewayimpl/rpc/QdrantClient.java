package org.carl.infrastructure.gatewayimpl.rpc;

import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.carl.components.qdrant.QdrantGrpcClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class QdrantClient {
    @Inject QdrantGrpcClient qdrantGrpcClient;

    private final Logger logger = Logger.getLogger(QdrantClient.class);

    public CompletionStage<Collections.CollectionOperationResponse> upsert(
            String collectionName, Long size, Collections.Distance distance) {
        return exists(collectionName)
                .thenCompose(
                        existsResp -> {
                            if (existsResp.getResult().getExists()) {
                                Collections.CollectionOperationResponse response =
                                        Collections.CollectionOperationResponse.newBuilder()
                                                .setResult(true)
                                                .build();
                                return CompletableFuture.completedFuture(response);
                            }
                            return create(collectionName, size, distance);
                        });
    }

    public CompletionStage<Collections.CollectionOperationResponse> create(
            String collectionName, Long size, Collections.Distance distance) {

        return qdrantGrpcClient
                .getCollectionsGrpcClient()
                .create(
                        builder -> {
                            Collections.VectorsConfig vectorsConfig =
                                    Collections.VectorsConfig.newBuilder()
                                            .setParams(
                                                    Collections.VectorParams.newBuilder()
                                                            .setSize(size)
                                                            .setDistance(distance)
                                                            .build())
                                            .build();
                            return builder.setCollectionName(collectionName)
                                    .setVectorsConfig(vectorsConfig)
                                    .build();
                        })
                .onSuccess(this::handleSuccess)
                .onFailure(this::handleFailure)
                .toCompletionStage();
    }

    public CompletionStage<Collections.CollectionExistsResponse> exists(String collectionName) {
        return qdrantGrpcClient
                .getCollectionsGrpcClient()
                .collectionExists(builder -> builder.setCollectionName(collectionName).build())
                .onSuccess(this::handleSuccess)
                .onFailure(this::handleFailure)
                .toCompletionStage();
    }

    public CompletionStage<Points.PointsOperationResponse> upsert(
            Integer id, String collectName, List<Float> embeddings) {
        return qdrantGrpcClient
                .getPointsGrpcClient()
                .upsert(
                        builder -> {
                            Points.PointId pointId = Points.PointId.newBuilder().setNum(id).build();
                            Points.Vector vector =
                                    Points.Vector.newBuilder()
                                            .setDense(
                                                    Points.DenseVector.newBuilder()
                                                            .addAllData(embeddings))
                                            .build();
                            Points.Vectors vectors =
                                    Points.Vectors.newBuilder().setVector(vector).build();
                            Points.PointStruct pointStruct =
                                    Points.PointStruct.newBuilder()
                                            .setId(pointId)
                                            .setVectors(vectors)
                                            .build();
                            return builder.setCollectionName(collectName)
                                    .addPoints(pointStruct)
                                    .build();
                        })
                .onSuccess(this::handleSuccess)
                .onFailure(this::handleFailure)
                .toCompletionStage();
    }

    public CompletionStage<Points.PointsOperationResponse> upsert(
            Long id, String collectName, List<Float> embeddings, Map<String, Object> payload) {
        return qdrantGrpcClient
                .getPointsGrpcClient()
                .upsert(
                        builder -> {
                            Points.PointId pointId = Points.PointId.newBuilder().setNum(id).build();
                            Points.Vector vector =
                                    Points.Vector.newBuilder()
                                            .setDense(
                                                    Points.DenseVector.newBuilder()
                                                            .addAllData(embeddings))
                                            .build();
                            Points.Vectors vectors =
                                    Points.Vectors.newBuilder().setVector(vector).build();
                            Map<String, JsonWithInt.Value> _payload = new HashMap<>();
                            payload.forEach(
                                    (k, v) -> {
                                        if (v instanceof String str) {
                                            _payload.put(
                                                    k,
                                                    JsonWithInt.Value.newBuilder()
                                                            .setStringValue(str)
                                                            .build());
                                            return;
                                        }
                                        if (v instanceof Boolean b) {
                                            _payload.put(
                                                    k,
                                                    JsonWithInt.Value.newBuilder()
                                                            .setBoolValue(b)
                                                            .build());
                                        }
                                    });
                            Points.PointStruct pointStruct =
                                    Points.PointStruct.newBuilder()
                                            .setId(pointId)
                                            .setVectors(vectors)
                                            .putAllPayload(_payload)
                                            .build();
                            return builder.setCollectionName(collectName)
                                    .addPoints(pointStruct)
                                    .build();
                        })
                .onSuccess(this::handleSuccess)
                .onFailure(this::handleFailure)
                .toCompletionStage();
    }

    public CompletionStage<Points.QueryResponse> query(
            String collectName, Points.PointId pointId, Integer limit, boolean withPayload) {

        return qdrantGrpcClient
                .getPointsGrpcClient()
                .query(
                        builder -> {
                            Points.Query query =
                                    Points.Query.newBuilder()
                                            .setNearest(
                                                    Points.VectorInput.newBuilder()
                                                            .setId(pointId)
                                                            .build())
                                            .build();
                            return builder.setCollectionName(collectName)
                                    .setQuery(query)
                                    .setLimit(limit)
                                    .setWithPayload(
                                            Points.WithPayloadSelector.newBuilder()
                                                    .setEnable(withPayload)
                                                    .build())
                                    .setWithVectors(
                                            Points.WithVectorsSelector.newBuilder()
                                                    .setEnable(false)
                                                    .build())
                                    .build();
                        })
                .onSuccess(this::handleSuccess)
                .onFailure(this::handleFailure)
                .toCompletionStage();
    }

    public Uni<Points.QueryResponse> query(
            String collectName, List<Float> embeddings, Integer limit, Boolean withPayload) {

        CompletionStage<Points.QueryResponse> completionStage =
                qdrantGrpcClient
                        .getPointsGrpcClient()
                        .query(
                                builder -> {
                                    Points.Query.Builder query =
                                            Points.Query.newBuilder()
                                                    .setNearest(
                                                            Points.VectorInput.newBuilder()
                                                                    .setDense(
                                                                            Points.DenseVector
                                                                                    .newBuilder()
                                                                                    .addAllData(
                                                                                            embeddings)));
                                    return builder.setCollectionName(collectName)
                                            .setQuery(query)
                                            .setLimit(limit)
                                            .setWithPayload(
                                                    Points.WithPayloadSelector.newBuilder()
                                                            .setEnable(withPayload))
                                            .setWithVectors(
                                                    Points.WithVectorsSelector.newBuilder()
                                                            .setEnable(false))
                                            .build();
                                })
                        .onSuccess(this::handleSuccess)
                        .onFailure(this::handleFailure)
                        .toCompletionStage();
        return Uni.createFrom().completionStage(completionStage);
    }
    public Uni<Points.QueryResponse> query(
            String collectName, List<Float> embeddings, Integer limit, Boolean withPayload, Points.Filter filter) {

        CompletionStage<Points.QueryResponse> completionStage =
                qdrantGrpcClient
                        .getPointsGrpcClient()
                        .query(
                                builder -> {
                                    Points.Query.Builder query =
                                            Points.Query.newBuilder()
                                                    .setNearest(
                                                            Points.VectorInput.newBuilder()
                                                                    .setDense(
                                                                            Points.DenseVector
                                                                                    .newBuilder()
                                                                                    .addAllData(
                                                                                            embeddings)));
                                    return builder.setCollectionName(collectName)
                                            .setQuery(query)
                                            .setFilter(filter)
                                            .setLimit(limit)
                                            .setWithPayload(
                                                    Points.WithPayloadSelector.newBuilder()
                                                            .setEnable(withPayload))
                                            .setWithVectors(
                                                    Points.WithVectorsSelector.newBuilder()
                                                            .setEnable(false))
                                            .build();
                                })
                        .onSuccess(this::handleSuccess)
                        .onFailure(this::handleFailure)
                        .toCompletionStage();
        return Uni.createFrom().completionStage(completionStage);
    }

    private void handleSuccess(Object o) {
        if (logger.isDebugEnabled()) {
            logger.debugf("grpc success: %s", o);
        }
    }

    private void handleFailure(Throwable throwable) {
        logger.errorf("grpc failure: %s", throwable.getMessage());
        if (logger.isDebugEnabled()) {
            logger.errorf(throwable, "grpc failure: %s", throwable.getMessage());
        }
    }
}
