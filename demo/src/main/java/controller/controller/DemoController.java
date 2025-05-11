package controller.controller;

import io.qdrant.client.grpc.Collections;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.carl.components.qdrant.QdrantGrpcClient;
import org.carl.infrastructure.component.web.annotations.ControllerLogged;
import org.carl.infrastructure.persistence.PersistenceStd;

import java.util.concurrent.CompletionStage;

@Path("/secure/demo")
@ControllerLogged
public class DemoController extends PersistenceStd {
  @Inject QdrantGrpcClient qdrantGrpcClient;

  @GET
  @Path("/hello")
  public String hello() {
    return "Hello World!";
  }

  @GET
  @Path("/get")
  public Uni<Response> get(@QueryParam("collectionName") String collectionName) {
    var completionStage =
        qdrantGrpcClient
            .getCollectionsGrpcClient()
            .get(builder -> builder.setCollectionName(collectionName).build())
            .toCompletionStage();

    return Uni.createFrom()
        .completionStage(completionStage)
        .onItem()
        .transform(item -> Response.ok(item.getResult().toString()).build());
  }

  @GET
  @Path("/create")
  public Uni<Response> create(@QueryParam("collectionName") String collectionName) {
    CompletionStage<Collections.CollectionOperationResponse> completionStage =
        qdrantGrpcClient
            .getCollectionsGrpcClient()
            .create(builder -> builder.setCollectionName(collectionName).build())
            .toCompletionStage();
    return Uni.createFrom()
        .completionStage(completionStage)
        .onItem()
        .transform(item -> Response.ok(item.getResult()).build());
  }
}
