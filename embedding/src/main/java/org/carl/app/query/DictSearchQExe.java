package org.carl.app.query;

import io.qdrant.client.grpc.Points;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.app.command.TextVectorUpsertCmdExe;
import org.carl.client.dto.DictSearchQ;
import org.carl.client.dto.VectorQ;
import org.carl.client.dto.clientobject.ScoredPointCO;
import org.carl.component.dto.MultiEntityResponse;

import static org.carl.components.factory.ConditionFactory.match;
import static org.carl.components.factory.ConditionFactory.matchKeyword;

@ApplicationScoped
public class DictSearchQExe {
    @Inject TextVectorUpsertCmdExe textVectorUpsertCmdExe;
    @Inject VectorQExe vectorQExe;

    public Uni<MultiEntityResponse<ScoredPointCO>> executer(DictSearchQ q) {
        return textVectorUpsertCmdExe
                .execute(q.getText())
                .onItem()
                .transformToUni(
                        item -> {
                            Points.Filter.Builder filter = Points.Filter.newBuilder();
                            if (q.getLevel() != null) {
                                filter.addMust(match("level", q.getLevel()));
                            }
                            filter.addMust(matchKeyword("dict_type", q.getDictType()));
                            VectorQ vectorQ =
                                    new VectorQ()
                                            .setFilter(filter.build())
                                            .setLimit(q.getLimit())
                                            .setCollectionName("dict_vectors")
                                            .setVector(item)
                                            .setWithPayload(true)
                                            .setWithVector(false);
                            return vectorQExe
                                    .executer(vectorQ)
                                    .onItem()
                                    .transform(
                                            MultiEntityResponse::of);
                        });
    }
}
