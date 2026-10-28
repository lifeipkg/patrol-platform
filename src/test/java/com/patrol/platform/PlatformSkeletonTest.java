package com.patrol.platform;

import com.patrol.platform.model.Decision;
import com.patrol.platform.model.PatrolVerdict;
import com.patrol.platform.pipeline.AbstractPatrolPipeline;
import com.patrol.platform.rule.Condition;
import com.patrol.platform.rule.Rule;
import com.patrol.platform.rule.RuleEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformSkeletonTest {

    @Test
    @DisplayName("规则引擎：按优先级短路求值，命中高优规则")
    void ruleEnginePriorityShortCircuit() {
        RuleEngine engine = new RuleEngine(List.of(
                new Rule("r_low", 10, true,
                        List.of(new Condition("risk_score", Condition.Op.GE, 100)), Decision.BAN, "R100"),
                new Rule("r_high", 1, true,
                        List.of(new Condition("keyword", Condition.Op.CONTAINS, "薇芯")), Decision.INTERCEPT, "KW_HIT")));

        PatrolVerdict v = engine.evaluate("evt-1", Map.of("keyword", "加.薇芯", "risk_score", 100));
        assertEquals(Decision.INTERCEPT, v.decision());
        assertEquals("KW_HIT", v.reason());
    }

    @Test
    @DisplayName("规则引擎：禁用规则不生效，未命中走默认放行")
    void ruleEngineDisabledAndDefault() {
        RuleEngine engine = new RuleEngine(List.of(
                new Rule("r_off", 1, false,
                        List.of(new Condition("risk_score", Condition.Op.GE, 10)), Decision.BAN, "OFF")),
                Decision.PASS);

        assertEquals(Decision.PASS, engine.evaluate("evt-2", Map.of("risk_score", 99)).decision());
    }

    @Test
    @DisplayName("巡查管线模板：四阶段同构骨架可被各巡查线复用")
    void pipelineTemplateMethod() {
        // 用"伪内容巡查线"验证骨架：归一化 -> 特征 -> 检测 -> 决策
        AbstractPatrolPipeline<String> pipeline = new AbstractPatrolPipeline<>() {
            @Override
            public String domain() {
                return "content";
            }

            @Override
            protected String normalize(String event) {
                return event.replace(" ", "").toLowerCase();
            }

            @Override
            protected Map<String, Object> extract(String normalized, String event) {
                return Map.of("hit", normalized.contains("vx"));
            }

            @Override
            protected Detection detect(Map<String, Object> features, String event) {
                boolean hit = (Boolean) features.get("hit");
                return hit ? new Detection(Decision.INTERCEPT, 80, "VARIANT_HIT")
                        : new Detection(Decision.PASS, 0, "OK");
            }

            @Override
            protected String eventIdOf(String event) {
                return "msg-" + event.length();
            }
        };

        assertEquals("content", pipeline.domain());
        assertEquals(Decision.INTERCEPT, pipeline.patrol("加 v x 123").decision());
        assertEquals(Decision.PASS, pipeline.patrol("正常消息").decision());
    }
}
