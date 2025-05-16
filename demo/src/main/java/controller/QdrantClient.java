package controller;

import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.components.qdrant.QdrantGrpcClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class QdrantClient {
    @Inject QdrantGrpcClient qdrantGrpcClient;

    public CompletionStage<Points.PointsOperationResponse> upsert(
            Integer id, String collectName, List<Float> embeddings, Map<String, Object> payload) {
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
                .toCompletionStage();
    }

    public CompletionStage<Points.QueryResponse> query(
            String collectName, List<Float> embeddings, Integer limit) {

        return qdrantGrpcClient
                .getPointsGrpcClient()
                .query(
                        builder -> {
                            Points.Query query =
                                    Points.Query.newBuilder()
                                            .setNearest(
                                                    Points.VectorInput.newBuilder()
                                                            .setDense(
                                                                    Points.DenseVector.newBuilder()
                                                                            .addAllData(embeddings))
                                                            .build())
                                            .build();
                            return builder.setCollectionName(collectName)
                                    .setQuery(query)
                                    .setLimit(limit)
                                    .build();
                        })
                .toCompletionStage();
    }
}
