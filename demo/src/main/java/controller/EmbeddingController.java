package controller;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import entity.QSimilarSearchKeyword;
import entity.SimilarSearchKeyword;
import io.embeddingj.client.grpc.EmbeddingOuterClass;
import io.qdrant.client.grpc.Points;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import model.FaceRequest;
import org.carl.infrastructure.component.web.annotations.ControllerLogged;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Path("/embedding")
@ControllerLogged
public class EmbeddingController {
    @Inject QdrantClient qdrantClient;
    @Inject EmbeddingClient embeddingClient;
    final String faceCollection = "face_vectors";
    @Inject EntityManager em;
    {
        QSimilarSearchKeyword similarSearchKeyword = QSimilarSearchKeyword.similarSearchKeyword;
//        JPAQueryFactory factory = new JPAQueryFactory(em);
    }
    //    @POST
    //    @Path("/text")
    //    public Uni<Response> search() {
    //        CompletionStage<EmbeddingOuterClass.TextVectorResponse> complet =
    //                embeddingGrpcClient
    //                        .textToVector(builder -> builder.setText("厨师").build())
    //                        .toCompletionStage();
    //        return Uni.createFrom()
    //                .completionStage(complet)
    //                .onItem()
    //                .transform(
    //                        item -> {
    //                            System.out.println(item.getVectorList());
    //                            return Response.ok().build();
    //                        });
    //    }

    // NOTE: https://cn.quarkus.io/guides/rest
    @POST
    @Path("/face/search")
    public Uni<Response> search(FaceRequest faceRequest) {
        CompletionStage<EmbeddingOuterClass.FaceVectorResponse> faceVectorResponseCompletionStage =
                embeddingClient.faceToVector(Base64.getDecoder().decode(faceRequest.getPhoto()));
        return Uni.createFrom()
                .completionStage(faceVectorResponseCompletionStage)
                .onItem()
                .transformToUni(
                        item -> {
                            CompletionStage<Points.QueryResponse> query =
                                    qdrantClient.query(faceCollection, item.getVectorList(), 5);
                            return Uni.createFrom().completionStage(query);
                        })
                .onItem()
                .transform(
                        item -> {
                            System.out.println(item);
                            return Response.ok().build();
                        });
    }

    @POST
    @Path("/face/upsert")
    public Uni<Response> upsert(FaceRequest faceRequest) {
        Map<String, Object> payload = faceRequest.getPayload();
        if (payload == null) {
            throw new RuntimeException("payload is null");
        }
        if (!payload.containsKey("id")) {
            throw new RuntimeException("id is null");
        }
        if (!payload.containsKey("idCard")) {
            throw new RuntimeException("idCard is null");
        }
        Object _id = payload.get("id");
        Integer id = (Integer) _id;
        CompletionStage<EmbeddingOuterClass.FaceVectorResponse> faceVectorResponseCompletionStage =
                embeddingClient.faceToVector(Base64.getDecoder().decode(faceRequest.getPhoto()));
        return Uni.createFrom()
                .completionStage(faceVectorResponseCompletionStage)
                .onItem()
                .transformToUni(
                        item -> {
                            CompletionStage<Points.PointsOperationResponse> upsert =
                                    qdrantClient.upsert(
                                            id, faceCollection, item.getVectorList(), payload);
                            return Uni.createFrom().completionStage(upsert);
                        })
                .onItem()
                .transform(
                        item -> {
                            System.out.println(item);
                            return Response.ok().build();
                        });
    }
}
