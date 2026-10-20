package org.carl.infra.ruleengine.qlexpress;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.QLResult;
import com.alibaba.qlexpress4.exception.QLErrorCodes;
import com.alibaba.qlexpress4.exception.QLException;
import com.alibaba.qlexpress4.exception.QLSyntaxException;
import com.alibaba.qlexpress4.exception.QLTimeoutException;
import com.alibaba.qlexpress4.runtime.context.DynamicVariableContext;
import com.alibaba.qlexpress4.runtime.trace.ExpressionTrace;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 直接使用 QLExpress 4 实现的高频业务场景，用于评估其能力与写法。
 *
 * <p>约定：事实统一用 Map 组织；Runner 全局共享（线程安全，内部缓存解析结果）；使用默认的隔离安全策略。
 */
class QLExpressScenarioTest {

    /** 业务系统里通常全局只有一个 Runner，Java 能力以自定义函数方式暴露给脚本。 */
    private static final Express4Runner RUNNER = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    /** 缓存解析结果；脚本中的赋值写回传入的 Map，便于读取多个输出。 */
    private static final QLOptions OPTIONS = QLOptions.builder().cache(true).polluteUserContext(true).build();

    static {
        Set<String> blacklist = Set.of("10.0.0.66", "10.0.0.88");
        RUNNER.addFunction("inBlacklist", (Function<String, Boolean>) blacklist::contains);
        Map<String, BigDecimal> pointRates = Map.of(
                "NORMAL", new BigDecimal("1"),
                "GOLD", new BigDecimal("1.5"),
                "DIAMOND", new BigDecimal("2"));
        RUNNER.addFunction("pointRate", (Function<String, BigDecimal>) level -> pointRates.getOrDefault(level, BigDecimal.ONE));
    }

    private static Object run(String script, Map<String, Object> context) {
        return RUNNER.execute(script, context, OPTIONS).getResult();
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    // ------------------------------------------------------------------ 营销

    /** 阶梯满减 + 会员折扣 + 优惠封顶。precise 模式下全部按 BigDecimal 计算，避免金额精度问题。 */
    @Test
    void tieredDiscount() {
        String script = """
                reduce = if (amount >= 500) then 100
                         else if (amount >= 300) then 50
                         else if (amount >= 100) then 15
                         else 0
                rate = if (user.level == 'VIP') then 0.95 else 1
                payable = (amount - reduce) * rate
                // 总优惠不超过订单金额的 30%
                maxDiscount = amount * 0.3
                if (amount - payable > maxDiscount) {
                    payable = amount - maxDiscount
                }
                payable
                """;
        QLOptions precise = QLOptions.builder().cache(true).precise(true).build();

        Object vip = RUNNER.execute(script, map("amount", 600, "user", map("level", "VIP")), precise).getResult();
        assertEquals(0, new BigDecimal("475").compareTo((BigDecimal) vip), "(600-100)*0.95");

        Object normal = RUNNER.execute(script, map("amount", 99.9, "user", map("level", "NORMAL")), precise).getResult();
        assertEquals(0, new BigDecimal("99.9").compareTo((BigDecimal) normal));

        // 0.1 + 0.2 这类 double 误差在 precise 模式下不存在
        assertTrue((Boolean) RUNNER.execute("a + b == 0.3", map("a", 0.1, "b", 0.2), precise).getResult());
    }

    /** 优惠券适用性：有效期、最低消费、可用品类，并计算可用品类商品的金额。 */
    @Test
    void couponApplicability() {
        String script = """
                eligibleItems = order.items.filter(i -> i.category in coupon.categories)
                eligibleAmount = 0
                for (item : eligibleItems) {
                    eligibleAmount += item.price * item.qty
                }
                // 换行会结束语句，跨行时操作符必须写在行尾
                usable = now >= coupon.startAt && now <= coupon.endAt &&
                         eligibleAmount >= coupon.minAmount
                usable
                """;
        Map<String, Object> coupon = map(
                "startAt", 1_000L, "endAt", 2_000L,
                "minAmount", 100,
                "categories", List.of("FOOD", "DRINK"));
        Map<String, Object> order = map("items", List.of(
                map("category", "FOOD", "price", 30, "qty", 2),
                map("category", "DRINK", "price", 50, "qty", 1),
                map("category", "DIGITAL", "price", 999, "qty", 1)));

        Map<String, Object> inTime = map("now", 1_500L, "coupon", coupon, "order", order);
        assertEquals(true, run(script, inTime));
        assertEquals(110, inTime.get("eligibleAmount"), "数码品类不计入");

        assertEquals(false, run(script, map("now", 3_000L, "coupon", coupon, "order", order)), "已过期");
    }

    // ------------------------------------------------------------------ 风控

    /** 风控评分卡：多条规则累加分数并记录命中原因，最后按分数分级。 */
    @Test
    void riskScorecard() {
        String script = """
                blacklisted = inBlacklist(order.ip)
                newUser = user.registerDays < 7
                bigOrderOfNewUser = order.amount > 5000 && user.registerDays < 30
                sharedDevice = device.accountCount >= 3
                score = (blacklisted ? 100 : 0) + (newUser ? 30 : 0) +
                        (bigOrderOfNewUser ? 40 : 0) + (sharedDevice ? 20 : 0)
                // 隔离策略下不能调用 list.add，用「条件列表 + filter」收集命中原因
                reasons = [
                    blacklisted ? 'IP 黑名单' : null,
                    newUser ? '新注册用户' : null,
                    bigOrderOfNewUser ? '新用户大额订单' : null,
                    sharedDevice ? '设备关联多账号' : null
                ].filter(r -> r != null)
                level = switch (true) {
                    case score >= 100 -> 'REJECT'
                    case score >= 50 -> 'REVIEW'
                    default -> 'PASS'
                }
                level
                """;

        Map<String, Object> risky = map(
                "user", map("registerDays", 3),
                "order", map("amount", 8000, "ip", "1.1.1.1"),
                "device", map("accountCount", 1));
        assertEquals("REVIEW", run(script, risky));
        assertEquals(70, risky.get("score"));
        assertEquals(List.of("新注册用户", "新用户大额订单"), risky.get("reasons"));

        Map<String, Object> blocked = map(
                "user", map("registerDays", 400),
                "order", map("amount", 10, "ip", "10.0.0.66"),
                "device", map("accountCount", 5));
        assertEquals("REJECT", run(script, blocked));

        Map<String, Object> normal = map(
                "user", map("registerDays", 400),
                "order", map("amount", 10, "ip", "1.1.1.1"),
                "device", map("accountCount", 1));
        assertEquals("PASS", run(script, normal));
        assertEquals(List.of(), normal.get("reasons"));
    }

    // ------------------------------------------------------------------ 审批 / 流程

    /** 审批路由：按金额和部门决定审批链，脚本直接返回审批人列表。 */
    @Test
    void approvalRouting() {
        String script = """
                [
                    'LEADER',
                    amount > 10000 ? 'DIRECTOR' : null,
                    amount > 100000 || dept == 'FINANCE' ? 'CFO' : null,
                    type == 'CONTRACT' ? 'LEGAL' : null
                ].filter(node -> node != null)
                """;

        assertEquals(List.of("LEADER"), run(script, map("amount", 500, "dept", "RD", "type", "EXPENSE")));
        assertEquals(List.of("LEADER", "DIRECTOR", "CFO", "LEGAL"),
                run(script, map("amount", 200000, "dept", "RD", "type", "CONTRACT")));
        assertEquals(List.of("LEADER", "CFO"), run(script, map("amount", 800, "dept", "FINANCE", "type", "EXPENSE")));
    }

    // ------------------------------------------------------------------ 计费 / 积分

    /** 运费：按地区查首重 / 续重价格表，满额包邮。价格表直接用 JSON 字面量配置在脚本里。 */
    @Test
    void shippingFee() {
        String script = """
                rates = {
                    "LOCAL":  {"first": 6,  "extra": 2},
                    "REMOTE": {"first": 15, "extra": 8}
                }
                rate = rates[region] == null ? rates['LOCAL'] : rates[region]
                // 首重 1kg，续重按整公斤向上取整。除法结果是 BigDecimal，需要 (int) 截断
                extraKg = weightGrams <= 1000 ? 0 : (int) ((weightGrams - 1000 + 999) / 1000)
                fee = rate.first + extraKg * rate.extra
                if (orderAmount >= 99 && region != 'REMOTE') { fee = 0 }
                fee
                """;

        assertEquals(6, run(script, map("region", "LOCAL", "weightGrams", 800, "orderAmount", 50)));
        assertEquals(10, run(script, map("region", "LOCAL", "weightGrams", 2500, "orderAmount", 50)));
        assertEquals(0, run(script, map("region", "LOCAL", "weightGrams", 2500, "orderAmount", 120)));
        assertEquals(31, run(script, map("region", "REMOTE", "weightGrams", 3000, "orderAmount", 120)), "偏远不包邮");
        assertEquals(6, run(script, map("region", "UNKNOWN", "weightGrams", 500, "orderAmount", 10)), "未知地区走默认");
    }

    /** 积分计算：会员倍率来自 Java 函数（如查配置中心），活动日双倍，单笔封顶。 */
    @Test
    void loyaltyPoints() {
        String script = """
                points = amount * pointRate(user.level)
                if (isPromotionDay) { points = points * 2 }
                points = if (points > 5000) then 5000 else points
                points
                """;
        QLOptions precise = QLOptions.builder().cache(true).precise(true).build();

        Object gold = RUNNER.execute(script, map("amount", 200, "user", map("level", "GOLD"), "isPromotionDay", true), precise)
                .getResult();
        assertEquals(0, new BigDecimal("600").compareTo((BigDecimal) gold));

        Object capped = RUNNER.execute(script, map("amount", 9999, "user", map("level", "DIAMOND"), "isPromotionDay", false), precise)
                .getResult();
        assertEquals(0, new BigDecimal("5000").compareTo(new BigDecimal(capped.toString())));
    }

    // ------------------------------------------------------------------ 指标 / 映射 / 模板

    /** 派生指标：动态变量按需计算并可嵌套引用，适合运营配置的指标口径。 */
    @Test
    void derivedMetrics() {
        Map<String, Object> raw = map("gmv", 120000, "orderCount", 400, "visitors", 10000, "refundAmount", 6000);
        DynamicVariableContext metrics = new DynamicVariableContext(RUNNER, raw, OPTIONS);
        metrics.put("客单价", "gmv / orderCount");
        metrics.put("转化率", "orderCount * 1.0 / visitors");
        metrics.put("退款率", "refundAmount * 1.0 / gmv");
        metrics.put("健康店铺", "转化率 >= 0.03 && 退款率 < 0.1 && 客单价 > 200");

        assertEquals(300, ((Number) RUNNER.execute("客单价", metrics, OPTIONS).getResult()).intValue());
        assertEquals(true, RUNNER.execute("健康店铺", metrics, OPTIONS).getResult());
    }

    /** 数据映射：把外部订单报文转换为内部模型，映射规则即一段 JSON 风格脚本。 */
    @Test
    void dataMapping() {
        String script = """
                {
                    "orderNo": src.trade_id,
                    "buyer": {"id": src.buyer.uid, "vip": src.buyer.level >= 3},
                    "totalCents": src.total * 100,
                    "skus": src.lines*.sku,
                    "status": switch (src.state) {
                        case 'WAIT_PAY' -> 'CREATED'
                        case 'PAID', 'SHIPPED' -> 'PROCESSING'
                        default -> 'UNKNOWN'
                    }
                }
                """;
        Map<String, Object> src = map(
                "trade_id", "T1001",
                "buyer", map("uid", 7, "level", 4),
                "total", 12,
                "state", "PAID",
                "lines", List.of(map("sku", "A1"), map("sku", "B2")));

        @SuppressWarnings("unchecked")
        Map<String, Object> target = (Map<String, Object>) run(script, map("src", src));

        assertEquals("T1001", target.get("orderNo"));
        assertEquals(map("id", 7, "vip", true), target.get("buyer"));
        assertEquals(1200, target.get("totalCents"));
        assertEquals(List.of("A1", "B2"), target.get("skus"));
        assertEquals("PROCESSING", target.get("status"));
    }

    /** 消息模板：通知文案由运营配置，支持插值与条件片段。 */
    @Test
    void notificationTemplate() {
        String template = "尊敬的${user.name}，您的订单 ${order.id} 已发货${if (order.express) then '（顺丰次日达）' else ''}，"
                + "实付 ${order.paid} 元。";

        Object express = RUNNER.executeTemplate(template,
                map("user", map("name", "张三"), "order", map("id", "T1001", "express", true, "paid", 88)), OPTIONS).getResult();
        assertEquals("尊敬的张三，您的订单 T1001 已发货（顺丰次日达），实付 88 元。", express);

        Object normal = RUNNER.executeTemplate(template,
                map("user", map("name", "李四"), "order", map("id", "T1002", "express", false, "paid", 9)), OPTIONS).getResult();
        assertEquals("尊敬的李四，您的订单 T1002 已发货，实付 9 元。", normal);
    }

    // ------------------------------------------------------------------ 规则平台能力

    /** 配置保存前：语法校验（带行列号），以及解析规则依赖的外部变量和函数，用于提示需要准备哪些事实。 */
    @Test
    void ruleConfigurationChecks() {
        String rule = """
                isNew = user.registerDays < 7
                isNew && order.amount > limit && inBlacklist(order.ip) == false
                """;

        RUNNER.check(rule);
        assertEquals(Set.of("user", "order", "limit"), RUNNER.getOutVarNames(rule));
        assertEquals(Set.of("inBlacklist"), RUNNER.getOutFunctions(rule));

        QLSyntaxException e = assertThrows(QLSyntaxException.class, () -> RUNNER.check("amount > 100 &&\n(level == 'VIP'"));
        assertEquals(2, e.getLineNo());
        assertEquals("SYNTAX_ERROR", e.getErrorCode());
    }

    /** 归因分析：规则返回 false 时，通过表达式追踪定位是哪个条件拦截的；短路未执行的节点标记为未求值。 */
    @Test
    void ruleAttributionByTrace() {
        Express4Runner tracing = new Express4Runner(InitOptions.builder().traceExpression(true).build());
        QLOptions traceOptions = QLOptions.builder().traceExpression(true).build();

        QLResult result = tracing.execute(
                "user.vip && order.amount >= 100 && user.loginDays <= 10",
                map("user", map("vip", true, "loginDays", 30), "order", map("amount", 50)),
                traceOptions);

        assertEquals(false, result.getResult());
        ExpressionTrace root = result.getExpressionTraces().getFirst();
        // (vip && amount>=100) && loginDays<=10：左侧为 false，右侧短路
        ExpressionTrace left = root.getChildren().get(0);
        ExpressionTrace amountCheck = left.getChildren().get(1);
        assertEquals(">=", amountCheck.getToken());
        assertEquals(false, amountCheck.getValue(), "被金额条件拦截");
        assertFalse(root.getChildren().get(1).isEvaluated(), "登录条件未求值");
        assertTrue(root.toPrettyString(0).contains("OPERATOR >= false"));
    }

    // ------------------------------------------------------------------ 安全与稳定性

    /** 运营配置的脚本不可信：默认隔离策略禁止访问 Java 方法 / 字段，超时防止死循环拖垮线程。 */
    @Test
    void untrustedScriptsAreSandboxed() {
        QLException exit = assertThrows(QLException.class, () -> run("System.exit(0)", map()));
        assertTrue(exit.getErrorCode().endsWith("NOT_FOUND"), exit.getErrorCode());

        QLException method = assertThrows(QLException.class, () -> run("name.length()", map("name", "abc")));
        assertEquals("METHOD_NOT_FOUND", method.getErrorCode());

        QLTimeoutException timeout = assertThrows(QLTimeoutException.class, () -> RUNNER.execute(
                "while (true) { 1 + 1 }", map(), QLOptions.builder().timeoutMillis(50L).build()));
        assertEquals(QLErrorCodes.SCRIPT_TIME_OUT.name(), timeout.getErrorCode());
    }

    /** 编写脚本时容易踩的坑（基于 4.1.3 实测），配置规则的文档 / 编辑器提示应当覆盖。 */
    @Test
    void syntaxPitfalls() {
        // 1. 换行即语句结束：操作符放在下一行行首会报语法错误，即使在括号内
        assertThrows(QLSyntaxException.class, () -> RUNNER.check("x = (1 > 0\n && 2 > 1)"));
        assertEquals(true, run("x = 1 > 0 &&\n 2 > 1\nx", map()));

        // 2. 列表不支持 + 拼接；隔离策略下 list.add / size、String.length 等 Java 方法都不可用
        assertThrows(QLException.class, () -> run("[1] + [2]", map()));
        assertThrows(QLException.class, () -> run("l = [1]; l.add(2)", map()));
        assertThrows(QLException.class, () -> run("[1, 2].size()", map()));
        // 内置扩展函数 filter / map 可用
        assertEquals(List.of(2, 4), run("[1, 2, 3, 4].filter(i -> i % 2 == 0)", map()));

        // 3. 不支持 elvis 运算符 ?:，用三元表达式代替
        assertThrows(QLSyntaxException.class, () -> RUNNER.check("a ?: 1"));

        // 4. 关键字（如 double、int）不能用作函数名或变量名
        assertThrows(QLSyntaxException.class, () -> RUNNER.check("double(1)"));

        // 5. 除法不是整数除法，结果为 BigDecimal（即使整除）；取整用 (int)，Math 类在隔离策略下不可用
        assertEquals(0, new BigDecimal("3.5").compareTo((BigDecimal) run("7 / 2", map())));
        assertEquals(BigDecimal.class, run("6 / 2", map()).getClass());
        assertEquals(3, run("(int) (7 / 2)", map()));
        assertThrows(QLException.class, () -> run("Math.ceil(2.1)", map()));

        // 6. 未赋值的变量读取为 null，不会报错，写错字段名不会被发现
        assertEquals(true, run("user.levle == null", map("user", map("level", "VIP"))));
    }

    /** 全局共享 Runner：多线程并发执行同一规则，每次使用独立的上下文 Map，结果互不干扰。 */
    @Test
    void sharedRunnerUnderConcurrency() throws Exception {
        String script = """
                reduce = if (amount >= 300) then 50 else 0
                payable = amount - reduce
                payable > 200 && user.level == 'VIP'
                """;
        int threads = 8;
        int perThread = 20_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                long seed = t;
                futures.add(pool.submit(() -> {
                    Random random = new Random(seed);
                    int wrong = 0;
                    for (int i = 0; i < perThread; i++) {
                        int amount = random.nextInt(500);
                        String level = random.nextBoolean() ? "VIP" : "NORMAL";
                        Map<String, Object> context = map("amount", amount, "user", map("level", level));
                        int payable = amount >= 300 ? amount - 50 : amount;
                        Object result = run(script, context);
                        if (!result.equals(payable > 200 && level.equals("VIP"))
                                || ((Number) context.get("payable")).intValue() != payable) {
                            wrong++;
                        }
                    }
                    return wrong;
                }));
            }
            for (Future<Integer> future : futures) {
                assertEquals(0, future.get(60, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
