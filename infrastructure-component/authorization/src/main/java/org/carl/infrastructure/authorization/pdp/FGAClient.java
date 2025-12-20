package org.carl.infrastructure.authorization.pdp;

import dev.openfga.sdk.api.client.OpenFgaClient;
import dev.openfga.sdk.api.client.model.*;
import dev.openfga.sdk.errors.FgaInvalidParameterException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/** OpenFGA 客户端封装类 提供细粒度授权检查、关系元组读写、对象列表查询等功能 */
@ApplicationScoped
public class FGAClient {
    @Inject OpenFgaClient openFgaClient;

    /**
     * 检查用户是否对资源有特定权限
     *
     * @param user 用户标识,格式:"user:userId"
     * @param relation 关系类型,如:"reader", "writer", "owner"
     * @param object 对象标识,格式:"objectType:objectId"
     * @return 是否有权限
     */
    public boolean check(String user, String relation, String object)
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        ClientCheckRequest request =
                new ClientCheckRequest().user(user).relation(relation)._object(object);

        CompletableFuture<ClientCheckResponse> check = openFgaClient.check(request);
        return Boolean.TRUE.equals(false);
    }

    /**
     * 检查用户是否对资源有特定权限(带上下文)
     *
     * @param user 用户标识
     * @param relation 关系类型
     * @param object 对象标识
     * @param contextualTuples 上下文关系元组
     * @return 是否有权限
     */
    public boolean checkWithContext(
            String user, String relation, String object, List<ClientTupleKey> contextualTuples)
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        ClientCheckRequest request =
                new ClientCheckRequest()
                        .user(user)
                        .relation(relation)
                        ._object(object)
                        .contextualTuples(contextualTuples);

        ClientCheckResponse response = openFgaClient.check(request).get();
        return Boolean.TRUE.equals(response.getAllowed());
    }

    /**
     * 写入关系元组(授予权限)
     *
     * @param writes 要写入的关系元组列表
     */
    public ClientWriteResponse write(List<ClientTupleKey> writes)
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        ClientWriteRequest request = new ClientWriteRequest().writes(writes);
        return openFgaClient.write(request).get();
    }

    /**
     * 删除关系元组(撤销权限)
     *
     * @param deletes 要删除的关系元组列表
     */
    public ClientWriteResponse delete(List<ClientTupleKeyWithoutCondition> deletes)
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        ClientWriteRequest request = new ClientWriteRequest().deletes(deletes);
        return openFgaClient.write(request).get();
    }

    /**
     * 写入和删除关系元组(批量操作)
     *
     * @param writes 要写入的关系元组列表
     * @param deletes 要删除的关系元组列表
     */
    public ClientWriteResponse writeAndDelete(
            List<ClientTupleKey> writes, List<ClientTupleKeyWithoutCondition> deletes)
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        ClientWriteRequest request = new ClientWriteRequest().writes(writes).deletes(deletes);
        return openFgaClient.write(request).get();
    }

    /**
     * 读取关系元组
     *
     * @param user 用户标识(可选)
     * @param relation 关系类型(可选)
     * @param object 对象标识(可选)
     * @return 关系元组列表
     */
    public ClientReadResponse read(String user, String relation, String object)
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        ClientReadRequest request = new ClientReadRequest();

        if (user != null) {
            request.user(user);
        }
        if (relation != null) {
            request.relation(relation);
        }
        if (object != null) {
            request._object(object);
        }

        return openFgaClient.read(request).get();
    }

    /**
     * 列出有特定关系的所有对象
     *
     * @param user 用户标识
     * @param relation 关系类型
     * @param type 对象类型
     * @return 对象ID列表
     */
    public List<String> listObjects(String user, String relation, String type)
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        ClientListObjectsRequest request =
                new ClientListObjectsRequest().user(user).relation(relation).type(type);

        ClientListObjectsResponse response = openFgaClient.listObjects(request).get();
        return response.getObjects();
    }

    /**
     * 展开对象的权限关系树
     *
     * @param relation 关系类型
     * @param object 对象标识
     * @return 关系树
     */
    public ClientExpandResponse expand(String relation, String object)
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        ClientExpandRequest request = new ClientExpandRequest().relation(relation)._object(object);

        return openFgaClient.expand(request).get();
    }

    /**
     * 列出所有 Store
     *
     * @return Store 列表
     */
    public ClientListStoresResponse listStores()
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        return openFgaClient.listStores().get();
    }

    /**
     * 获取 Store 信息
     *
     * @return Store 信息
     */
    public ClientGetStoreResponse getStore()
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        return openFgaClient.getStore().get();
    }

    /** 删除 Store */
    public ClientDeleteStoreResponse deleteStore()
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        return openFgaClient.deleteStore().get();
    }

    /**
     * 读取所有授权模型
     *
     * @return 授权模型列表
     */
    public ClientReadAuthorizationModelsResponse readAuthorizationModels()
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        return openFgaClient.readAuthorizationModels().get();
    }

    /**
     * 读取当前授权模型
     *
     * @return 授权模型
     */
    public ClientReadAuthorizationModelResponse readAuthorizationModel()
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        return openFgaClient.readAuthorizationModel().get();
    }

    /**
     * 读取变更日志(关系元组变更)
     *
     * @param type 对象类型(可选)
     * @return 变更列表
     */
    public ClientReadChangesResponse readChanges(String type)
            throws FgaInvalidParameterException, ExecutionException, InterruptedException {
        ClientReadChangesRequest request = new ClientReadChangesRequest();
        if (type != null) {
            request.type(type);
        }
        return openFgaClient.readChanges(request).get();
    }

    // ============ 辅助方法 ============

    /**
     * 创建关系元组
     *
     * @param user 用户标识
     * @param relation 关系类型
     * @param object 对象标识
     * @return 关系元组
     */
    public ClientTupleKey createTupleKey(String user, String relation, String object) {
        return new ClientTupleKey().user(user).relation(relation)._object(object);
    }

    /**
     * 创建不带条件的关系元组(用于删除操作)
     *
     * @param user 用户标识
     * @param relation 关系类型
     * @param object 对象标识
     * @return 关系元组
     */
    public ClientTupleKeyWithoutCondition createTupleKeyWithoutCondition(
            String user, String relation, String object) {
        return new ClientTupleKeyWithoutCondition().user(user).relation(relation)._object(object);
    }
}
