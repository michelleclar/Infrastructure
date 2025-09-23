package org.carl.infrastructure.gatewayimpl;

import com.google.protobuf.Timestamp;

import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import io.smallrye.mutiny.Uni;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.carl.client.dto.cmd.PointUpsertCmd;
import org.carl.client.dto.query.RecommendationQ;
import org.carl.components.factory.*;
import org.carl.components.qdrant.QdrantGrpcClient;
import org.carl.domain.gateway.IEmbeddingGateway;
import org.carl.domain.gateway.IQdrantGateway;
import org.carl.domain.qdrant.PointsQuery;
import org.carl.domain.qdrant.VectorCollection;
import org.carl.domain.qdrant.filter.Filter;
import org.carl.domain.qdrant.filter.condition.Condition;
import org.carl.domain.qdrant.filter.condition.DataTimeRange;
import org.carl.infrastructure.ability.IGrpcClientLogAbility;
import org.carl.infrastructure.common.exception.QdrantException;
import org.carl.infrastructure.common.exception.RecommendException;
import org.carl.infrastructure.utils.CollectionUtils;
import org.carl.infrastructure.utils.StringUtils;
import org.carl.utils.Assert;

import java.util.*;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class QdrantGatewayImpl implements IQdrantGateway, IGrpcClientLogAbility {
    @Inject QdrantGrpcClient qdrantGrpcClient;
    @Inject IEmbeddingGateway embeddingGateway;

    @Override
    public Uni<Integer> pointUpsert(String collection, List<Points.PointStruct> points) {
        return this.qdrantPointUpsert(collection, points)
                .onFailure()
                .transform(QdrantException::qdrantUpsertException);
    }

    @Override
    public Uni<List<Points.ScoredPoint>> pointsQuery(String collection, PointsQuery q) {
        return this.qdrantPointsQuery(collection, q)
                .onFailure()
                .transform(QdrantException::qdrantQueryException);
    }

    @Override
    public Uni<Boolean> collectionUpsert(VectorCollection collection) {
        return this.collectionUpsert(collection.name(), collection.getVectorsConfig())
                .onFailure()
                .transform(QdrantException::qdrantCollectionCreateException);
    }

    @Override
    public Uni<Boolean> collectionUpsert(
            String collectionName, int size, Collections.Distance distance) {
        return this.collectionUpsert(collectionName, VectorsConfigFactory.build(size, distance));
    }

    @Override
    public Uni<Integer> pointUpsert(PointUpsertCmd pointUpsertCmd) {

        Map<String, Map<String, Object>> data = pointUpsertCmd.getData();
        List<String> includeVectorFields = pointUpsertCmd.getIncludeVectorFields();
        List<Uni<Points.PointStruct>> points = handle(data, includeVectorFields);
        return Uni.join()
                .all(points)
                .usingConcurrencyOf(1)
                .andFailFast()
                .onFailure()
                .transform(QdrantException::qdrantUpsertException)
                .onItem()
                .transformToUni(item -> this.pointUpsert(pointUpsertCmd.getCollectionName(), item))
                .onFailure()
                .transform(QdrantException::qdrantUpsertException);
    }

    @Override
    public Uni<List<Points.ScoredPoint>> recommend(RecommendationQ q) {
        Uni<Points.RecommendInput> recommendInputUni =
                handleRecommend(
                        q.getStrategy(),
                        q.getPositiveIds(),
                        q.getPositiveVectors(),
                        q.getPositiveVectorTexts(),
                        q.getNegativeIds(),
                        q.getNegativeVectors(),
                        q.getNegativeVectorTexts());
        PointsQuery pointsQuery =
                new PointsQuery()
                        .setLimit(q.getLimit())
                        .setOffset(q.getOffset())
                        .setWithPayload(q.isWithPayload())
                        .setWithVector(q.isWithVector());
        if (q.getFilter() != null) {
            Points.Filter filter = handleFilter(q.getFilter());
            pointsQuery.setFilter(filter);
        }
        return Uni.createFrom()
                .item(pointsQuery)
                .onItem()
                .transformToUni(
                        item ->
                                recommendInputUni
                                        .onItem()
                                        .transform(
                                                recommendInput -> {
                                                    item.setQuery(
                                                            QueryFactory.recommend(recommendInput));
                                                    return item;
                                                }))
                .onItem()
                .transformToUni(query -> this.pointsQuery(q.getVectorCollection(), query));
    }

    private Uni<Points.RecommendInput> handleRecommend(
            Points.RecommendStrategy strategy,
            List<String> positiveIds,
            List<List<Float>> positiveVectors,
            List<String> positiveVectorTexts,
            List<String> negativeIds,
            List<List<Float>> negativeVectors,
            List<String> negativeVectorTexts) {
        // positive
        Uni<List<Points.VectorInput>> positive =
                handleRelationVector(positiveIds, positiveVectors, positiveVectorTexts);
        // negative
        Uni<List<Points.VectorInput>> negative =
                handleRelationVector(negativeIds, negativeVectors, negativeVectorTexts);

        return Uni.createFrom()
                .item(Points.RecommendInput.newBuilder())
                .onItem()
                .transform(
                        recommendInput -> {
                            recommendInput.setStrategy(strategy);
                            return recommendInput;
                        })
                .onItem()
                .transformToUni(
                        recommendInput ->
                                positive.onItem()
                                        .transform(
                                                pos -> {
                                                    recommendInput.addAllPositive(pos);
                                                    return recommendInput;
                                                }))
                .onItem()
                .transformToUni(
                        recommendInput ->
                                negative.onItem()
                                        .transform(
                                                neg -> {
                                                    recommendInput.addAllNegative(neg);
                                                    return recommendInput.build();
                                                }));
    }

    private Points.Filter handleFilter(Filter filter) {
        Points.Filter.Builder builder = Points.Filter.newBuilder();
        List<Condition> must = filter.getMust();
        if (CollectionUtils.notEmpty(must)) {
            Points.Filter mustFilter = mustFilter(must);
            builder.addAllMust(mustFilter.getMustList());
        }
        List<Condition> should = filter.getShould();
        if (CollectionUtils.notEmpty(should)) {
            Points.Filter shouldFilter = shouldFilter(should);
            builder.addAllShould(shouldFilter.getShouldList());
        }
        List<Condition> mustNot = filter.getMustNot();
        if (CollectionUtils.notEmpty(mustNot)) {
            Points.Filter mustNotFilter = mustNotFilter(mustNot);
            builder.addAllMustNot(mustNotFilter.getMustList());
        }
        return builder.build();
    }

    private List<Points.Condition> handleCondition(@Nonnull List<Condition> conditions) {
        List<Points.Condition> r = new ArrayList<>();
        for (Condition condition : conditions) {
            if (condition.getMatchKeyword() != null) {
                Points.Condition con =
                        ConditionFactory.matchKeyword(
                                condition.getMatchKeyword().getField(),
                                condition.getMatchKeyword().getKeyword());
                r.add(con);
            }
            if (condition.getDatetimeRange() != null) {
                DataTimeRange datetimeRange = condition.getDatetimeRange();
                Points.DatetimeRange.Builder datetimeRangeBuilder =
                        Points.DatetimeRange.newBuilder();
                if (datetimeRange.getGt() != null) {
                    datetimeRangeBuilder.setGt(
                            Timestamp.newBuilder()
                                    .setSeconds(datetimeRange.getGt().getTime())
                                    .build());
                }
                if (datetimeRange.getLt() != null) {
                    datetimeRangeBuilder.setLt(
                            Timestamp.newBuilder()
                                    .setSeconds(datetimeRange.getLt().getTime())
                                    .build());
                }
                if (datetimeRange.getGte() != null) {
                    datetimeRangeBuilder.setGte(
                            Timestamp.newBuilder()
                                    .setSeconds(datetimeRange.getGte().getTime())
                                    .build());
                }
                if (datetimeRange.getLte() != null) {
                    datetimeRangeBuilder.setLte(
                            Timestamp.newBuilder()
                                    .setSeconds(datetimeRange.getLte().getTime())
                                    .build());
                }
                Points.Condition dateCon =
                        ConditionFactory.datetimeRange(
                                datetimeRange.getField(), datetimeRangeBuilder.build());
                r.add(dateCon);
            }
            if (condition.getIsNull() != null) {
                Points.Condition nullCon = ConditionFactory.isNull(condition.getIsNull());
                r.add(nullCon);
            }
            if (condition.getIsEmpty() != null) {
                Points.Condition emptyCon = ConditionFactory.isEmpty(condition.getIsEmpty());
                r.add(emptyCon);
            }
            if (CollectionUtils.notEmpty(condition.getHisIds())) {
                List<Points.PointId> ids =
                        condition.getHisIds().stream().map(PointIdFactory::id).toList();
                Points.Condition hasIdsCon = ConditionFactory.hasId(ids);
                r.add(hasIdsCon);
            }
        }
        return r;
    }

    private Points.Filter shouldFilter(@Nonnull List<Condition> should) {
        Points.Filter.Builder builder = Points.Filter.newBuilder();
        List<Points.Condition> conditions = handleCondition(should);
        if (CollectionUtils.notEmpty(conditions)) {
            conditions.forEach(builder::addShould);
        }
        return builder.build();
    }

    private Points.Filter mustFilter(@Nonnull List<Condition> must) {
        Points.Filter.Builder filterBuild = Points.Filter.newBuilder();
        List<Points.Condition> conditions = handleCondition(must);
        if (CollectionUtils.notEmpty(conditions)) {
            conditions.forEach(filterBuild::addMust);
        }
        return filterBuild.build();
    }

    private Points.Filter mustNotFilter(@Nonnull List<Condition> must) {
        Points.Filter.Builder filterBuild = Points.Filter.newBuilder();
        List<Points.Condition> conditions = handleCondition(must);
        if (CollectionUtils.notEmpty(conditions)) {
            conditions.forEach(filterBuild::addMustNot);
        }
        return filterBuild.build();
    }

    private Uni<List<Points.VectorInput>> handleRelationVector(
            List<String> ids, List<List<Float>> vectors, List<String> vectorTexts) {
        List<Points.VectorInput> vectorInputs = new ArrayList<>();
        if (CollectionUtils.notEmpty(ids)) {
            ids.forEach(
                    item ->
                            vectorInputs.add(
                                    VectorInputFactory.vectorInput(PointIdFactory.id(item))));
        }
        List<Uni<Points.VectorInput>> vectorUnis = new ArrayList<>();
        if (CollectionUtils.notEmpty(vectorTexts)) {
            vectorTexts.forEach(
                    item -> {
                        Uni<Points.VectorInput> vectorInputUni =
                                Uni.createFrom()
                                        .item(item)
                                        .onItem()
                                        .transformToUni(
                                                text ->
                                                        embeddingGateway
                                                                .textToVector(text)
                                                                .onItem()
                                                                .transform(
                                                                        response ->
                                                                                VectorInputFactory
                                                                                        .vectorInput(
                                                                                                response
                                                                                                        .getVectorList())));
                        vectorUnis.add(vectorInputUni);
                    });
        }

        if (CollectionUtils.notEmpty(vectors)) {
            vectors.forEach(item -> vectorInputs.add(VectorInputFactory.vectorInput(item)));
        }
        if (vectorUnis.isEmpty()) {
            return Uni.createFrom().item(vectorInputs);
        }

        return Uni.join()
                .all(vectorUnis)
                .usingConcurrencyOf(1)
                .andFailFast()
                .onFailure()
                .transform(RecommendException::recommendQueryBuildException)
                .onItem()
                .transform(
                        list -> {
                            list.addAll(vectorInputs);
                            return list;
                        });
    }

    private List<Uni<Points.PointStruct>> handle(
            Map<String, Map<String, Object>> data, List<String> fields) {
        Assert.assertNotEmpty(fields, "vector fields can not be empty");
        if (fields.size() == 1) {
            return handleSingle(data, fields.getFirst());
        }
        return handleMulti(data, fields);
    }

    private List<Uni<Points.PointStruct>> handleSingle(
            Map<String, Map<String, Object>> data, String field) {
        List<Uni<Points.PointStruct>> points = new ArrayList<>();
        // TODO: need support multi
        data.forEach(
                (k, v) -> {
                    // NOTE: must exist and must string
                    if (!v.containsKey(field)) {
                        throw QdrantException.qdrantUpsertException(
                                "Vector field '" + field + "' is not exist");
                    }
                    Object o = v.get(field);
                    if (o instanceof String str) {
                        Uni<Points.PointStruct> transform =
                                embeddingGateway
                                        .textToVector(str)
                                        .onItem()
                                        .transform(
                                                item ->
                                                        StringUtils.isNumeric(k)
                                                                ? PointStructFactory
                                                                        .buildPointStruct(
                                                                                Long.parseLong(k),
                                                                                item
                                                                                        .getVectorList(),
                                                                                v)
                                                                : PointStructFactory
                                                                        .buildPointStruct(
                                                                                UUID.fromString(k),
                                                                                item
                                                                                        .getVectorList(),
                                                                                v));
                        points.add(transform);
                        return;
                    }
                    throw QdrantException.qdrantUpsertException(
                            "Vector field '" + field + "' is not a string");
                });
        return points;
    }

    private List<Uni<Points.PointStruct>> handleMulti(
            Map<String, Map<String, Object>> data, List<String> fields) {
        List<Uni<Points.PointStruct>> points = new ArrayList<>();
        data.forEach(
                (k, v) -> {
                    List<Uni<Pair<String, Points.Vector>>> vectors = new ArrayList<>();
                    fields.forEach(
                            field -> {
                                if (!v.containsKey(field)) {
                                    throw QdrantException.qdrantUpsertException(
                                            "Vector field '" + field + "' is not exist");
                                }
                                Object o = v.get(field);
                                if (o instanceof String str) {
                                    Uni<Pair<String, Points.Vector>> vector =
                                            embeddingGateway
                                                    .textToVector(str)
                                                    .onItem()
                                                    .transform(
                                                            item ->
                                                                    Pair.of(
                                                                            field,
                                                                            VectorFactory.vector(
                                                                                    item
                                                                                            .getVectorList())));
                                    vectors.add(vector);
                                    return;
                                }
                                throw QdrantException.qdrantUpsertException(
                                        "Vector field '" + field + "' is not a string");
                            });
                    Uni<Points.PointStruct> transform =
                            Uni.join()
                                    .all(vectors)
                                    .usingConcurrencyOf(1)
                                    .andFailFast()
                                    .onFailure()
                                    .transform(QdrantException::qdrantUpsertException)
                                    .onItem()
                                    .transform(
                                            item -> {
                                                Map<String, Points.Vector> _vectors =
                                                        new HashMap<>();
                                                item.forEach(
                                                        pair ->
                                                                _vectors.put(
                                                                        pair.getLeft(),
                                                                        pair.getRight()));
                                                return _vectors;
                                            })
                                    .onItem()
                                    .transform(
                                            item ->
                                                    StringUtils.isNumeric(k)
                                                            ? PointStructFactory.buildPointStruct(
                                                                    Long.parseLong(k), item, v)
                                                            : PointStructFactory.buildPointStruct(
                                                                    UUID.fromString(k), item, v));
                    points.add(transform);
                });
        return points;
    }

    /**
     * UnknownUpdateStatus 0
     *
     * <p>Acknowledged 1 Update is received, but not processed yet
     *
     * <p>Completed 2 Update is applied and ready for search
     *
     * <p>ClockRejected 3 Internal: update is rejected due to an outdated clock
     *
     * @param collectionName vector collectionName
     * @param points point
     * @return updateType
     */
    private Uni<Integer> qdrantPointUpsert(String collectionName, List<Points.PointStruct> points) {
        CompletionStage<Points.PointsOperationResponse> upsert =
                qdrantGrpcClient
                        .getPointsGrpcClient()
                        .upsert(
                                builder ->
                                        builder.setCollectionName(collectionName)
                                                .addAllPoints(points)
                                                .build())
                        .onSuccess(this::grpcClientSuccess)
                        .onFailure(this::grpcClientFailure)
                        .toCompletionStage();
        return Uni.createFrom()
                .completionStage(upsert)
                .onItem()
                .transform(item -> item.getResult().getStatus().getNumber());
    }

    private Uni<List<Points.ScoredPoint>> qdrantPointsQuery(String collectionName, PointsQuery q) {
        CompletionStage<Points.QueryResponse> completionStage =
                qdrantGrpcClient
                        .getPointsGrpcClient()
                        .query(
                                builder -> {
                                    builder.setCollectionName(collectionName)
                                            .setQuery(q.getQuery())
                                            .setLimit(q.getLimit())
                                            .setWithPayload(
                                                    WithPayloadSelectorFactory.enable(
                                                            q.getWithPayload()))
                                            .setWithVectors(
                                                    WithVectorsSelectorFactory.enable(
                                                            q.getWithVector()));
                                    if (q.getFilter() != null) builder.setFilter(q.getFilter());
                                    return builder.build();
                                })
                        .onSuccess(this::grpcClientSuccess)
                        .onFailure(this::grpcClientFailure)
                        .toCompletionStage();
        return Uni.createFrom()
                .completionStage(completionStage)
                .onItem()
                .transform(Points.QueryResponse::getResultList);
    }

    private Uni<Collections.CollectionExistsResponse> collectionExists(String collectionName) {
        CompletionStage<Collections.CollectionExistsResponse> completionStage =
                qdrantGrpcClient
                        .getCollectionsGrpcClient()
                        .collectionExists(
                                builder -> builder.setCollectionName(collectionName).build())
                        .onSuccess(this::grpcClientSuccess)
                        .onFailure(this::grpcClientFailure)
                        .toCompletionStage();
        return Uni.createFrom().completionStage(completionStage);
    }

    private Uni<Boolean> collectionCreate(
            String collectionName, Collections.VectorsConfig vectorsConfig) {
        CompletionStage<Collections.CollectionOperationResponse> completionStage =
                qdrantGrpcClient
                        .getCollectionsGrpcClient()
                        .create(
                                builder ->
                                        builder.setCollectionName(collectionName)
                                                .setVectorsConfig(vectorsConfig)
                                                .build())
                        .onSuccess(this::grpcClientSuccess)
                        .onFailure(this::grpcClientFailure)
                        .toCompletionStage();
        return Uni.createFrom()
                .completionStage(completionStage)
                .onItem()
                .transform(Collections.CollectionOperationResponse::getResult);
    }

    private Uni<Boolean> collectionUpsert(
            String collectionName, Collections.VectorsConfig vectorsConfig) {
        return collectionExists(collectionName)
                .onItem()
                .transformToUni(
                        item -> {
                            if (item.getResult().getExists()) {
                                return Uni.createFrom().item(true);
                            }
                            return collectionCreate(collectionName, vectorsConfig);
                        });
    }

    @Override
    public String grpcClientName() {
        return "grpc.client.qdrant";
    }
}
