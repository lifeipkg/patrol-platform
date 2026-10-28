package com.patrol.platform.pipeline;

import com.patrol.platform.model.PatrolVerdict;

/**
 * 巡查管线统一抽象。
 *
 * 平台设计核心：内容/消息/账号/资金四条巡查线业务完全不同，
 * 但接入与产出是同构的 —— 都是「事件进、PatrolVerdict 出」，
 * 因此接入层（Kafka 消费、幂等、状态机）与处置层（分级处置、复核、回流）完全复用。
 *
 * @param <T> 各巡查线自己的事件类型
 */
public interface PatrolPipeline<T> {

    /** 事件类型标识，与 Kafka topic 一一对应，如 "content" / "message" / "account" / "fund"。 */
    String domain();

    /** 执行巡查：事件进、结论出。实现必须幂等（同一 eventId 重复调用结果一致）。 */
    PatrolVerdict patrol(T event);
}
