package org.carl.app.command;

import io.qdrant.client.grpc.Points;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.client.dto.VectorUpsertCmd;
import org.carl.components.qdrant.QdrantGrpcClient;
import org.carl.infrastructure.component.web.config.exception.BizException;
import org.carl.infrastructure.component.web.config.exception.ExceptionReason;
import org.jboss.logging.Logger;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class VectorUpsertCmdExe {
    @Inject QdrantGrpcClient qdrantGrpcClient;
    private static final Logger logger = Logger.getLogger(VectorUpsertCmdExe.class);

    public Uni<Points.PointsOperationResponse> execute(VectorUpsertCmd vectorUpsertCmd) {
        Objects.requireNonNull(
                vectorUpsertCmd.getCollectionName(), "collectionName can not be null");
        Objects.requireNonNull(vectorUpsertCmd.getPoints(), "points can not be null");
        CompletionStage<Points.PointsOperationResponse> completionStage =
                qdrantGrpcClient
                        .getPointsGrpcClient()
                        .upsert(
                                builder -> builder.setCollectionName(
                                                vectorUpsertCmd.getCollectionName())
                                        .addAllPoints(vectorUpsertCmd.getPoints())
                                        .build())
                        .onFailure(
                                throwable -> {
                                    logger.errorv(throwable.getMessage(), throwable);
                                    throw new BizException(
                                            ExceptionReason.biz(
                                                    throwable.getMessage(),
                                                    "qdrant.upsert",
                                                    "qdrant.upsert",
                                                    99999));
                                })
                        .toCompletionStage();
        return Uni.createFrom().completionStage(completionStage);
    }
}
