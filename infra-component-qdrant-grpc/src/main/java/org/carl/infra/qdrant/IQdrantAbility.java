package org.carl.infra.qdrant;

import org.carl.infra.qdrant.clents.CollectionsGrpcClient;
import org.carl.infra.qdrant.clents.PointsGrpcClient;

public interface IQdrantAbility {

    QdrantGrpcClient getQdrantClient();

    default PointsGrpcClient getPoints() {
        return getQdrantClient().getPointsGrpcClient();
    }

    default CollectionsGrpcClient getCollections() {
        return getQdrantClient().getCollectionsGrpcClient();
    }
}
