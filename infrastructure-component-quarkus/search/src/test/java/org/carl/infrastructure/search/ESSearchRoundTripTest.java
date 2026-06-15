package org.carl.infrastructure.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.carl.infrastructure.search.plugins.es.core.action.Indices;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 端到端集成测试：通过 ISearchAbility 文档化入口，对真实 ES（Quarkus Dev Services 经本机 Docker 拉起）
 * 跑通 建索引 → 写入 → 按 ID 读取 → 全文搜索 → 删除 全链路。
 */
@QuarkusTest
class ESSearchRoundTripTest {

    static final String INDEX = "articles-it";

    @Inject ArticleSearchService search;

    @Test
    void indexGetSearchDelete() {
        // 1. 创建索引并指定 mapping（Indices 直接拿底层 indices client）
        Indices indices =
                new Indices(search.getSearchOperations().getESContext().getClient().indices());
        indices.create(
                c ->
                        c.index(INDEX)
                                .mappings(
                                        m ->
                                                m.properties("title", p -> p.text(t -> t))
                                                        .properties("content", p -> p.text(t -> t))
                                                        .properties(
                                                                "status", p -> p.keyword(k -> k))));

        // 2. 写入文档；refresh=true 保证立即可被搜索到
        Article article = new Article("Java 入门指南", "学习 Java 并发与 JVM 调优", "active");
        IndexRequest.Builder<Article> indexBuilder = new IndexRequest.Builder<>();
        indexBuilder.refresh(Refresh.True);
        IndexResponse indexResponse =
                search.index(indexBuilder)
                        .index(INDEX)
                        .id("doc-001")
                        .document(article)
                        .executor();
        assertEquals("doc-001", indexResponse.id());

        // 3. 按 ID 读取
        Article got =
                search.get(new GetRequest.Builder())
                        .index(INDEX)
                        .id("doc-001")
                        .fetchOf(Article.class);
        assertNotNull(got);
        assertEquals("active", got.status());
        assertEquals("Java 入门指南", got.title());

        // 4. 全文搜索
        List<Article> hits =
                search.search(new SearchRequest.Builder())
                        .index(INDEX)
                        .query(q -> q.match(m -> m.field("title").query("Java")))
                        .fetchOf(Article.class);
        assertEquals(1, hits.size());
        assertEquals("Java 入门指南", hits.get(0).title());

        // 5. 删除
        DeleteResponse deleteResponse =
                search.delete(new DeleteRequest.Builder())
                        .index(INDEX)
                        .id("doc-001")
                        .executor();
        assertEquals(Result.Deleted, deleteResponse.result());

        // 6. 删除索引：存在则真正删除并返回 acknowledged
        DeleteIndexResponse dropped = indices.delete(INDEX);
        assertNotNull(dropped);
        assertTrue(dropped.acknowledged());

        // 7. 幂等删除：索引已不存在时静默返回 null（不抛 index_not_found_exception）
        assertNull(indices.delete(INDEX));
    }

    @Test
    void upsertInsertsThenUpdates() {
        String idx = "articles-upsert-it";

        // 文档不存在：docAsUpsert 直接按 doc 插入 → Created
        Article draft = new Article("发布计划", "第一版草稿", "draft");
        UpdateResponse<Article> created =
                search.update(new UpdateRequest.Builder<Article, Article>())
                        .index(idx)
                        .id("a-1")
                        .upsert(draft)
                        .executor();
        assertEquals(Result.Created, created.result());

        // GET by id 是实时的，无需 refresh 即可读到
        Article afterInsert =
                search.get(new GetRequest.Builder()).index(idx).id("a-1").fetchOf(Article.class);
        assertNotNull(afterInsert);
        assertEquals("draft", afterInsert.status());
        assertEquals("第一版草稿", afterInsert.content());

        // 文档已存在：按 doc 部分更新 → Updated
        Article published = new Article("发布计划", "第二版定稿", "active");
        UpdateResponse<Article> updated =
                search.update(new UpdateRequest.Builder<Article, Article>())
                        .index(idx)
                        .id("a-1")
                        .upsert(published)
                        .executor();
        assertEquals(Result.Updated, updated.result());

        Article afterUpdate =
                search.get(new GetRequest.Builder()).index(idx).id("a-1").fetchOf(Article.class);
        assertEquals("active", afterUpdate.status());
        assertEquals("第二版定稿", afterUpdate.content());

        // 清理
        new Indices(search.getSearchOperations().getESContext().getClient().indices()).delete(idx);
    }
}
