package com.patrol.platform.model;

/** 统一巡查决策：所有巡查线共用，处置层按决策分级执行。 */
public enum Decision {
    /** 放行 */
    PASS,
    /** 限流（如当日触达次数受限） */
    LIMIT,
    /** 加审（转人工/二次审核后才可分发） */
    EXTRA_REVIEW,
    /** 拦截 */
    INTERCEPT,
    /** 封禁（针对账号级处置） */
    BAN
}
