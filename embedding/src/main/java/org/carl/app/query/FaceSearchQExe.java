package org.carl.app.query;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.client.dto.FaceSearchQ;
import org.carl.client.dto.VectorQ;
import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.component.dto.MultiEntityResponse;
import org.carl.infrastructure.component.web.config.exception.BizException;
import org.carl.infrastructure.gatewayimpl.rpc.EmbeddingClient;
import org.carl.utils.Base64Utils;

@ApplicationScoped
public class FaceSearchQExe {
    @Inject EmbeddingClient embeddingClient;
    @Inject VectorQExe vectorQExe;

    public Uni<MultiEntityResponse<ScoredPointCO>> executer(FaceSearchQ q) {

        return embeddingClient
                .faceToVector(Base64Utils.decode(q.getPhoto()))
                .onItem()
                .transformToUni(
                        Unchecked.function(
                                item -> {
                                    if (item.getCode() == 9999) {
                                        throw new BizException(
                                                org.carl.infrastructure.component.web.config
                                                        .exception.ExceptionReason.biz(
                                                        item.getReason(),
                                                        "embedding.face",
                                                        "embedding.face",
                                                        item.getCode()));
                                    }
                                    VectorQ vectorQ =
                                            new VectorQ()
                                                    .setWithPayload(false)
                                                    .setVector(item.getVectorList())
                                                    .setLimit(1)
                                                    .setCollectionName("face_vectors");
                                    return vectorQExe.executer(vectorQ);
                                }))
                .onItem()
                .transform(
                        MultiEntityResponse::of);
    }
}
