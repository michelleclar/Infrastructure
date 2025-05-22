package org.carl.adapter;

import io.qdrant.client.grpc.Collections;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.util.concurrent.CompletionStage;

import org.carl.client.api.IEmbeddingServer;
import org.carl.client.dto.FaceUpsertCmd;
import org.carl.component.dto.EntityResponse;
import org.carl.component.dto.MultiEntityResponse;
import org.carl.infrastructure.gatewayimpl.rpc.EmbeddingClient;
import org.carl.infrastructure.gatewayimpl.rpc.QdrantClient;
import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.infrastructure.component.web.annotations.ControllerLogged;
import org.carl.client.dto.DictSearchQ;
import org.carl.client.dto.FaceSearchQ;
import org.jboss.logging.Logger;

@Path("/embedding")
@ControllerLogged
public class EmbeddingController {
    private final Logger logger = Logger.getLogger(EmbeddingController.class);
    @Inject QdrantClient qdrantClient;
    @Inject EmbeddingClient embeddingClient;
    @Inject IEmbeddingServer embeddingServer;
    final String faceCollection = "face_vectors";
    final String dictCollection = "dict_vectors";
    final String postCollection = "post_vectors";

    @PostConstruct
    public void init() {
        CompletionStage<Collections.CollectionOperationResponse> face =
                qdrantClient.upsert(faceCollection, 512L, Collections.Distance.Cosine);
        Uni.createFrom()
                .completionStage(face)
                .subscribe()
                .with(
                        Unchecked.consumer(
                                response -> {
                                    if (response.getResult()) {
                                        return;
                                    }
                                    logger.errorf("response %s", response);
                                    logger.errorf("Failed to create collection %s", faceCollection);
                                    throw new RuntimeException();
                                }));
        CompletionStage<Collections.CollectionOperationResponse> text =
                qdrantClient.upsert(dictCollection, 1024L, Collections.Distance.Cosine);
        Uni.createFrom()
                .completionStage(text)
                .subscribe()
                .with(
                        Unchecked.consumer(
                                response -> {
                                    if (response.getResult()) {
                                        return;
                                    }
                                    logger.errorf("response %s", response);
                                    logger.errorf("Failed to create collection %s", faceCollection);
                                    throw new RuntimeException();
                                }));
    }

    @POST
    @Path("/dict/search")
    public Uni<MultiEntityResponse<ScoredPointCO>> dictSearch(DictSearchQ dictSearchQ) {
        return embeddingServer.dictSearch(dictSearchQ);
    }

    // NOTE: https://cn.quarkus.io/guides/rest
    @POST
    @Path("/face/search")
    public Uni<MultiEntityResponse<ScoredPointCO>> faceSearch(FaceSearchQ faceSearchQ) {
        return embeddingServer.faceSearch(faceSearchQ);
        //        return embeddingClient
        //                .faceToVector(Base64Utils.decode(faceSearchQ.getPhoto()))
        //                .onItem()
        //                .transformToUni(
        //                        Unchecked.function(
        //                                item -> {
        //                                    if (item.getCode() == 9999) {
        //                                        throw new BizException(
        //                                                ExceptionReason.create(
        //                                                        item.getReason(),
        //                                                        "embedding.face",
        //                                                        item.getCode()),
        //                                                "embedding.face");
        //                                    }
        //                                    return qdrantClient.query(
        //                                            faceCollection, item.getVectorList(), 1,
        // false);
        //                                }))
        //                .onItem()
        //                .transform(
        //                        item -> {
        //                            List<Points.ScoredPoint> resultList = item.getResultList();
        //                            List<ScoredPointCO> list =
        //                                    resultList.stream()
        //                                            .map(
        //                                                    o -> {
        //                                                        return
        // ScoredPointConvertor.toClientObject(
        //                                                                o);
        //                                                    })
        //                                            .toList();
        //                            return
        // Response.ok(SingleEntityResponse.of(list.getFirst())).build();
        //                        });
    }

    @POST
    @Path("/face/upsert")
    public Uni<EntityResponse> faceUpsert(FaceUpsertCmd faceUpsertCmd) {
        return embeddingServer.faceUpsert(faceUpsertCmd);
        //        Map<String, Object> payload = faceUpsertCmd.getPayload();
        //        if (payload == null) {
        //            throw new RuntimeException("payload is null");
        //        }
        //        if (!payload.containsKey("id")) {
        //            throw new RuntimeException("id is null");
        //        }
        //        Object _id = payload.get("id");
        //        Integer id = (Integer) _id;
        //        return embeddingClient
        //                .faceToVector(Base64Utils.decode(faceUpsertCmd.getPhoto()))
        //                .onItem()
        //                .transformToUni(
        //                        item -> {
        //                            CompletionStage<Points.PointsOperationResponse> upsert =
        //                                    qdrantClient.upsert(id, faceCollection,
        // item.getVectorList());
        //                            return Uni.createFrom().completionStage(upsert);
        //                        })
        //                .onItem()
        //                .transform(item ->
        // Response.ok(SingleEntityResponse.of("success")).build());
    }
}
