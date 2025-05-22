package org.carl.app.command;

import io.qdrant.client.grpc.Points;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.client.dto.FaceUpsertCmd;
import org.carl.client.dto.VectorUpsertCmd;
import org.carl.component.dto.EntityResponse;
import org.carl.infrastructure.gatewayimpl.rpc.EmbeddingClient;
import org.carl.utils.Base64Utils;
import org.carl.utils.PointStructFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class FaceUpsertCmdExe {
    @Inject VectorUpsertCmdExe vectorUpsertCmdExe;
    @Inject EmbeddingClient embeddingClient;

    public Uni<EntityResponse> execute(FaceUpsertCmd cmd) {
        Map<String, Object> payload = cmd.getPayload();
        if (payload == null) {
            throw new RuntimeException("payload is null");
        }
        if (!payload.containsKey("id")) {
            throw new RuntimeException("id is null");
        }
        Object _id = payload.get("id");
        Integer id = (Integer) _id;
        return embeddingClient
                .faceToVector(Base64Utils.decode(cmd.getPhoto()))
                .onItem()
                .transformToUni(
                        item -> {
                            VectorUpsertCmd vectorUpsertCmd =
                                    new VectorUpsertCmd().setCollectionName("face_vectors");
                            Points.PointStruct pointStruct =
                                    PointStructFactory.buildPointStruct(id, item.getVectorList());
                            List<Points.PointStruct> points = new ArrayList<>();
                            points.add(pointStruct);
                            vectorUpsertCmd.setPoints(points);

                            return vectorUpsertCmdExe.execute(vectorUpsertCmd);
                        })
                .onItem()
                .transform(item -> EntityResponse.buildSuccess());
    }
}
