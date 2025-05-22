package org.carl.app.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.app.command.FaceUpsertCmdExe;
import org.carl.app.query.DictSearchQExe;
import org.carl.app.query.FaceSearchQExe;
import org.carl.app.query.ResumeSearchQExe;
import org.carl.client.api.IEmbeddingServer;
import org.carl.client.dto.DictSearchQ;
import org.carl.client.dto.FaceSearchQ;
import org.carl.client.dto.FaceUpsertCmd;
import org.carl.client.dto.ResumeSearchQ;
import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.component.dto.EntityResponse;
import org.carl.component.dto.MultiEntityResponse;

@ApplicationScoped
public class EmbeddingServerImpl implements IEmbeddingServer {
    @Inject DictSearchQExe dictSearchQExe;
    @Inject FaceSearchQExe faceSearchQExe;
    @Inject FaceUpsertCmdExe faceUpsertCmdExe;
    @Inject ResumeSearchQExe resumeSearchQExe;

    @Override
    public Uni<MultiEntityResponse<ScoredPointCO>> dictSearch(DictSearchQ dictSearchQ) {
        return dictSearchQExe.executer(dictSearchQ);
    }

    @Override
    public Uni<MultiEntityResponse<ScoredPointCO>> faceSearch(FaceSearchQ faceSearchQ) {
        return faceSearchQExe.executer(faceSearchQ);
    }

    @Override
    public Uni<EntityResponse> faceUpsert(FaceUpsertCmd faceUpsertCmd) {
        return faceUpsertCmdExe.execute(faceUpsertCmd);
    }

    @Override
    public Uni<MultiEntityResponse<ScoredPointCO>> resumeSearch(ResumeSearchQ resumeSearchQ) {
        return null;
    }
}
