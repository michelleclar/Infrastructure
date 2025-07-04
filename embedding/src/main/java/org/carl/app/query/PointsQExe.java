package org.carl.app.query;

import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;

import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.client.dto.query.PointsQ;
import org.carl.component.dto.MultiEntityResponse;

@ApplicationScoped
public class PointsQExe {

    public Uni<MultiEntityResponse<ScoredPointCO>> executer(PointsQ q) {
        return Uni.createFrom().emitter(emitter -> {});
    }
}
