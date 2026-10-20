package org.carl.infra.ruleengine;

import com.alibaba.qlexpress4.exception.QLSyntaxException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 场景：前端以 JSON 提交规则配置（ruleId + 条件树），后端解析、编译校验后把「原始条件树 JSON」存库；
 * 之后前端再以 JSON 请求执行（ruleId + facts），后端按 ruleId 取回条件 JSON，用同一工厂 executor
 * 重新解析/编译并计算，返回 {@code {"ruleId": ..., "matched": ...}}。
 *
 * <p>说明：这里的 handleSave / handleExecute / FakeRuleStore 都是嵌在测试里的业务 mock，
 * facts 视为可信生产后端给出的数据；全程无 HTTP、无真实持久化、无外部服务。
 */
class FrontendJsonScenarioTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 默认构造绑定 ExpressionFactory.getDefault()，保存与执行共用同一 executor。 */
    private static final ConditionCompiler COMPILER = new ConditionCompiler();
    private static final ExpressionExecutor EXECUTOR = ExpressionFactory.getDefault();

    /** 前端「保存规则」请求：VIP 且（金额 >= 100 或 地区在名单内）的嵌套 AND/OR 条件树。 */
    private static final String SAVE_REQUEST = """
            {
              "ruleId": "promo.vip-gift",
              "condition": {
                "combinator": "and",
                "rules": [
                  { "field": "user.level", "operator": "==", "value": "VIP" },
                  {
                    "combinator": "or",
                    "rules": [
                      { "field": "amount", "operator": ">=", "value": 100 },
                      { "field": "region", "operator": "in", "value": ["BJ", "SH", "GZ"] }
                    ]
                  }
                ]
              }
            }
            """;

    /** 前端「保存直接脚本」请求：脚本原文以 JSON 字符串保存（\\n 是 JSON 转义，解析后为换行）。 */
    private static final String SCRIPT_SAVE_REQUEST = """
            {
              "ruleId": "promo.discount",
              "script": "discount = amount >= 300 ? 50 : 0\\ndiscount"
            }
            """;

    /** 模拟数据库：ruleId -> 提交时的原始文本（条件树 JSON 或脚本原文）。只存字符串，绝不存 runner 或编译产物。 */
    private static final class FakeRuleStore {
        private final Map<String, String> rows = new HashMap<>();

        void save(String ruleId, String text) {
            rows.put(ruleId, text);
        }

        String find(String ruleId) {
            String text = rows.get(ruleId);
            if (text == null) {
                throw new IllegalStateException("规则不存在: " + ruleId);
            }
            return text;
        }
    }

    /** 从请求 JSON 取非空文本字段；根节点结构校验用 JsonNode，避免非受检强转。 */
    private static String requireTextField(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw new IllegalArgumentException("请求缺少文本字段: " + field);
        }
        return node.asText();
    }

    /** 模拟「保存规则」接口：解析请求 -> 结构校验 -> 保存前编译校验 -> 只把原始条件树 JSON 文本入库。 */
    private static void handleSave(String requestJson, FakeRuleStore store) throws Exception {
        JsonNode root = MAPPER.readTree(requestJson);
        String ruleId = requireTextField(root, "ruleId");
        JsonNode condition = root.get("condition");
        if (condition == null || !condition.isObject()) {
            throw new IllegalArgumentException("请求缺少 condition 对象");
        }
        // 保存前编译校验：条件树非法（未知操作符、结构畸形等）在此抛出，绝不落库
        COMPILER.compile(MAPPER.convertValue(condition, new TypeReference<Map<String, Object>>() { }));
        store.save(ruleId, condition.toString());
    }

    /**
     * 模拟「执行规则」接口：解析请求 -> 按 ruleId 取回条件 JSON -> 重新解析/编译（与保存同一工厂 executor）
     * -> 用请求 facts 计算 -> 返回 {"ruleId", "matched"} JSON。
     */
    private static String handleExecute(String requestJson, FakeRuleStore store) throws Exception {
        JsonNode root = MAPPER.readTree(requestJson);
        String ruleId = requireTextField(root, "ruleId");
        JsonNode factsNode = root.get("facts");
        if (factsNode == null || !factsNode.isObject()) {
            throw new IllegalArgumentException("请求缺少 facts 对象");
        }
        Map<String, Object> facts = MAPPER.readValue(factsNode.toString(), new TypeReference<Map<String, Object>>() { });

        Map<String, Object> tree = MAPPER.readValue(store.find(ruleId), new TypeReference<Map<String, Object>>() { });
        ConditionCompiler.CompiledCondition compiled = COMPILER.compile(tree);
        Object matched = EXECUTOR.execute(compiled, facts);
        return MAPPER.writeValueAsString(Map.of("ruleId", ruleId, "matched", matched));
    }

    /** 提交执行请求，解析响应并只取 matched 布尔值（完整响应结构断言见核心流程用例）。 */
    private static boolean executeAndMatched(FakeRuleStore store, String requestJson) throws Exception {
        JsonNode response = MAPPER.readTree(handleExecute(requestJson, store));
        assertTrue(response.get("matched").isBoolean(), "响应必须是 {ruleId, matched} JSON: " + response);
        return response.get("matched").asBoolean();
    }

    /** 模拟「保存直接脚本」接口：check(script) 通过才入库，存的是脚本原文。 */
    private static void handleScriptSave(String requestJson, FakeRuleStore store) throws Exception {
        JsonNode root = MAPPER.readTree(requestJson);
        String ruleId = requireTextField(root, "ruleId");
        JsonNode scriptNode = root.get("script");
        if (scriptNode == null || !scriptNode.isTextual()) {
            throw new IllegalArgumentException("请求缺少 script 文本");
        }
        EXECUTOR.check(scriptNode.asText());
        store.save(ruleId, scriptNode.asText());
    }

    /** 模拟「执行直接脚本」接口：读取脚本原文并执行，返回脚本结果（折扣值）。 */
    private static Object handleScriptExecute(String ruleId, Map<String, Object> facts, FakeRuleStore store) {
        return EXECUTOR.execute(store.find(ruleId), facts);
    }

    // ------------------------------------------------------------------ 核心流程

    /** 保存 -> 原样 roundtrip（编辑器可重载）-> 执行 true / false / 阈值边界，多次调用互不影响。 */
    @Test
    void saveThenExecuteRoundTripsAndMatchesAcrossIndependentCalls() throws Exception {
        FakeRuleStore store = new FakeRuleStore();
        JsonNode conditionNode = MAPPER.readTree(SAVE_REQUEST).get("condition");

        handleSave(SAVE_REQUEST, store);
        String stored = store.find("promo.vip-gift");

        // 库里存的是原始条件树 JSON 而非编译产物：读回后与提交的 condition 完全一致，编辑器可原样重载
        JsonNode reloaded = MAPPER.readTree(stored);
        assertEquals(conditionNode, reloaded, "roundtrip 后条件树必须与提交时一致");
        assertFalse(stored.contains("__condition_"), "不允许把编译参数写入库");
        assertFalse(stored.contains("&&"), "不允许把生成的 QLExpress 脚本写入库");

        // 保存期与执行期编译必须得到相同脚本，且走工厂的同一个默认 executor
        ConditionCompiler.CompiledCondition savedCompile = COMPILER.compile(
                MAPPER.convertValue(conditionNode, new TypeReference<Map<String, Object>>() { }));
        ConditionCompiler.CompiledCondition reloadCompile = COMPILER.compile(
                MAPPER.readValue(stored, new TypeReference<Map<String, Object>>() { }));
        assertEquals(savedCompile.script(), reloadCompile.script(), "重新编译必须得到相同脚本");
        assertSame(EXECUTOR, ExpressionFactory.get(ExpressionOptions.DEFAULT), "保存与执行共用工厂默认 executor");

        // 执行请求 = ruleId + facts；响应为 {ruleId, matched} JSON
        String response = handleExecute("""
                {"ruleId":"promo.vip-gift","facts":{"user":{"level":"VIP"},"amount":150,"region":"SH"}}""", store);
        JsonNode responseNode = MAPPER.readTree(response);
        assertEquals("promo.vip-gift", responseNode.get("ruleId").asText());
        assertTrue(responseNode.get("matched").isBoolean());
        assertTrue(responseNode.get("matched").asBoolean(), "VIP + 150 + 地区命中 -> matched");

        // 以下每次调用都是独立请求、独立 facts，结果互不串扰
        assertEquals(false, executeAndMatched(store, """
                {"ruleId":"promo.vip-gift","facts":{"user":{"level":"BASIC"},"amount":150,"region":"SH"}}"""),
                "非 VIP -> 不命中");
        // 阈值边界：>= 100，amount == 100 命中，99 不命中
        assertEquals(true, executeAndMatched(store, """
                {"ruleId":"promo.vip-gift","facts":{"user":{"level":"VIP"},"amount":100,"region":"SZ"}}"""),
                "amount == 100 是边界，应命中");
        assertEquals(false, executeAndMatched(store, """
                {"ruleId":"promo.vip-gift","facts":{"user":{"level":"VIP"},"amount":99,"region":"SZ"}}"""),
                "amount == 99 且地区不在名单 -> 不命中");
        // OR 分支：金额不足但地区在名单内 -> 命中
        assertEquals(true, executeAndMatched(store, """
                {"ruleId":"promo.vip-gift","facts":{"user":{"level":"VIP"},"amount":50,"region":"BJ"}}"""),
                "金额不足但地区在名单 -> 命中");
    }

    // ------------------------------------------------------------------ 直接脚本（独立小场景）

    /** 脚本以 JSON 保存：保存前 check 语法；执行得到简单折扣输出，且脚本内赋值不写回 facts（无副作用）。 */
    @Test
    void directScriptSavedAsJsonPassesCheckThenExecutesDiscountWithoutSideEffects() throws Exception {
        FakeRuleStore store = new FakeRuleStore();
        handleScriptSave(SCRIPT_SAVE_REQUEST, store);
        assertEquals("discount = amount >= 300 ? 50 : 0\ndiscount", store.find("promo.discount"),
                "库里存的是脚本原文（JSON 转义已还原）");

        Map<String, Object> facts = MAPPER.readValue(
                "{\"amount\": 350}", new TypeReference<Map<String, Object>>() { });
        assertEquals(50, ((Number) handleScriptExecute("promo.discount", facts, store)).intValue(), "满 300 减 50");
        assertEquals(0, ((Number) handleScriptExecute("promo.discount",
                MAPPER.readValue("{\"amount\": 299}", new TypeReference<Map<String, Object>>() { }),
                store)).intValue(), "未达门槛折扣为 0");

        // 无副作用：脚本内 discount 赋值不会写回调用方的 facts
        assertEquals(Map.of("amount", 350), facts);

        // 语法不过的脚本在保存期被 check 拒绝，且不影响已入库配置
        String badScriptRequest = "{\"ruleId\": \"promo.bad\", \"script\": \"amount >= 300 ?\"}";
        assertThrows(QLSyntaxException.class, () -> handleScriptSave(badScriptRequest, store));
        assertThrows(IllegalStateException.class, () -> store.find("promo.bad"), "被拒绝的脚本不得入库");
        assertEquals("discount = amount >= 300 ? 50 : 0\ndiscount", store.find("promo.discount"));
    }

    // ------------------------------------------------------------------ 错误路径

    /** 非法 JSON 由 Jackson 报错，且不产生任何入库写入。 */
    @Test
    void invalidJsonIsRejectedByJacksonBeforeAnyStoreWrite() {
        FakeRuleStore store = new FakeRuleStore();
        assertThrows(JsonParseException.class, () -> handleSave("{ \"ruleId\": \"x\", ", store));
        assertThrows(JsonParseException.class, () -> handleExecute("{ \"facts\": ", store));
        assertThrows(JsonParseException.class, () -> handleScriptSave("not-json", store));
        assertThrows(IllegalStateException.class, () -> store.find("x"), "非法请求不得产生写入");
    }

    /** 条件树畸形 / 未知操作符在保存期编译失败，已入库的合法配置必须原样保留。 */
    @Test
    void malformedConditionOrUnknownOperatorFailsSaveAndKeepsStoredConfigIntact() throws Exception {
        FakeRuleStore store = new FakeRuleStore();
        handleSave(SAVE_REQUEST, store);
        String stored = store.find("promo.vip-gift");

        // 未知操作符：覆盖保存同一 ruleId 必须失败，且库里仍是旧配置
        String unknownOperator = """
                {
                  "ruleId": "promo.vip-gift",
                  "condition": {
                    "combinator": "and",
                    "rules": [
                      { "field": "user.level", "operator": "==", "value": "VIP" },
                      { "field": "amount", "operator": "exec", "value": 100 }
                    ]
                  }
                }
                """;
        IllegalArgumentException operatorError =
                assertThrows(IllegalArgumentException.class, () -> handleSave(unknownOperator, store));
        assertTrue(operatorError.getMessage().contains("unsupported operator"),
                "message: " + operatorError.getMessage());
        assertEquals(stored, store.find("promo.vip-gift"), "保存失败不得覆盖已入库配置");

        // 结构畸形：叶子缺少 value 字段
        String missingValue = "{\"ruleId\": \"promo.vip-gift\", \"condition\": "
                + "{\"field\": \"amount\", \"operator\": \">=\", \"bad\": 1}}";
        IllegalArgumentException structureError =
                assertThrows(IllegalArgumentException.class, () -> handleSave(missingValue, store));
        assertTrue(structureError.getMessage().contains("expected group or leaf"),
                "message: " + structureError.getMessage());
        assertEquals(stored, store.find("promo.vip-gift"));

        // 未知组合器
        String unknownCombinator = "{\"ruleId\": \"promo.vip-gift\", \"condition\": "
                + "{\"combinator\": \"xor\", \"rules\": [{\"field\": \"amount\", \"operator\": \">=\", \"value\": 1}]}}";
        IllegalArgumentException combinatorError =
                assertThrows(IllegalArgumentException.class, () -> handleSave(unknownCombinator, store));
        assertTrue(combinatorError.getMessage().contains("combinator must be and or or"),
                "message: " + combinatorError.getMessage());
        assertEquals(stored, store.find("promo.vip-gift"));
    }

    /** 未知 ruleId 必须显式报错并指明 ruleId，而不是返回 null 或误导性异常。 */
    @Test
    void unknownRuleIdFailsWithExplicitError() throws Exception {
        FakeRuleStore store = new FakeRuleStore();
        handleSave(SAVE_REQUEST, store);

        String request = "{\"ruleId\": \"promo.not-exists\", \"facts\": {\"amount\": 100}}";
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> handleExecute(request, store));
        assertTrue(error.getMessage().contains("promo.not-exists"), "message: " + error.getMessage());
    }

    // ------------------------------------------------------------------ 字面量绑定与参数注入

    /** 条件树里的字面量按值绑定为保留参数；facts 不得占用保留命名空间注入/覆盖这些字面量。 */
    @Test
    void literalValuesStayBoundAsParametersAndInjectionViaFactsIsRejected() throws Exception {
        FakeRuleStore store = new FakeRuleStore();
        handleSave(SAVE_REQUEST, store);

        ConditionCompiler.CompiledCondition compiled = COMPILER.compile(
                MAPPER.readValue(store.find("promo.vip-gift"), new TypeReference<Map<String, Object>>() { }));

        // 前端 JSON 里的字面量原样保留为绑定值：执行时以绑定的字面量为准
        assertEquals(3, compiled.parameters().size(), "三个叶子各绑定一个字面量");
        assertEquals(Set.of("VIP", 100, List.of("BJ", "SH", "GZ")),
                new HashSet<>(compiled.parameters().values()),
                "绑定值必须与 JSON 字面量完全一致");
        assertEquals(true, EXECUTOR.execute(compiled,
                Map.of("user", Map.of("level", "VIP"), "amount", 100, "region", "BJ")));

        // facts 试图用保留参数命名空间注入/覆盖字面量 -> 显式拒绝
        IllegalArgumentException injection = assertThrows(IllegalArgumentException.class,
                () -> EXECUTOR.execute(compiled, new HashMap<>(Map.of("__condition_0", "x", "amount", 1))));
        assertTrue(injection.getMessage().contains("reserved parameter namespace"),
                "message: " + injection.getMessage());
    }
}
