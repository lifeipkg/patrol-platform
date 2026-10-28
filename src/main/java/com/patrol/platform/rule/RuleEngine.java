package com.patrol.platform.rule;

import com.patrol.platform.model.Decision;
import com.patrol.platform.model.PatrolVerdict;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 轻量规则引擎（骨架演示）。
 *
 * 求值语义：
 *   1. 规则按 priority 升序（数字小者优先）逐条求值；
 *   2. 第一条命中的规则生效（短路），返回其决策；
 *   3. 无规则命中返回默认放行。
 *
 * 生产化差异（见 sql/rules-schema.sql）：
 *   - 规则存 MySQL，管理端可配置，变更后原子切换规则快照（不可变 List，读多写少无锁）；
 *   - 高 QPS 事件可叠加"特征预计算 + 规则表达式编译"，本骨架保持可读性未做。
 */
public class RuleEngine {

    public static final String REASON_DEFAULT = "NO_RULE_HIT";

    private final List<Rule> sortedRules;
    private final Decision defaultDecision;

    public RuleEngine(List<Rule> rules) {
        this(rules, Decision.PASS);
    }

    public RuleEngine(List<Rule> rules, Decision defaultDecision) {
        List<Rule> sorted = rules == null ? new ArrayList<>() : new ArrayList<>(rules);
        sorted.sort(Comparator.comparingInt(Rule::priority));
        this.sortedRules = List.copyOf(sorted);
        this.defaultDecision = defaultDecision;
    }

    /** 对事实求值：返回首个命中规则的决策。 */
    public PatrolVerdict evaluate(String eventId, Map<String, Object> facts) {
        for (Rule rule : sortedRules) {
            if (rule.match(facts)) {
                return new PatrolVerdict(eventId, rule.decision(), scoreOf(rule), rule.reason(), Map.of());
            }
        }
        return new PatrolVerdict(eventId, defaultDecision, 0, REASON_DEFAULT, Map.of());
    }

    public List<Rule> rules() {
        return sortedRules;
    }

    private static int scoreOf(Rule rule) {
        return switch (rule.decision()) {
            case PASS -> 0;
            case LIMIT -> 40;
            case EXTRA_REVIEW -> 60;
            case INTERCEPT -> 80;
            case BAN -> 100;
        };
    }
}
