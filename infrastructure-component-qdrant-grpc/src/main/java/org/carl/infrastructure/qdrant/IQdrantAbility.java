package org.carl.infrastructure.qdrant;

import org.carl.infrastructure.qdrant.clents.CollectionsGrpcClient;
import org.carl.infrastructure.qdrant.clents.PointsGrpcClient;

public interface IQdrantAbility {

    QdrantGrpcClient getQdrantClient();

    default PointsGrpcClient getPoints() {
        return getQdrantClient().getPointsGrpcClient();
    }

    default CollectionsGrpcClient getCollections() {
        return getQdrantClient().getCollectionsGrpcClient();
    }
}
