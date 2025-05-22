package org.carl.client.api;

import io.smallrye.mutiny.Uni;
import org.carl.client.dto.DictSearchQ;
import org.carl.client.dto.FaceSearchQ;
import org.carl.client.dto.FaceUpsertCmd;
import org.carl.client.dto.ResumeSearchQ;
import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.component.dto.EntityResponse;
import org.carl.component.dto.MultiEntityResponse;

// NOTE: embedding server interface
public interface IEmbeddingServer {
    Uni<MultiEntityResponse<ScoredPointCO>> dictSearch(DictSearchQ dictSearchQ);

    Uni<MultiEntityResponse<ScoredPointCO>> faceSearch(FaceSearchQ faceSearchQ);

    Uni<EntityResponse> faceUpsert(FaceUpsertCmd faceUpsertCmd);

    Uni<MultiEntityResponse<ScoredPointCO>> resumeSearch(ResumeSearchQ resumeSearchQ);
}
