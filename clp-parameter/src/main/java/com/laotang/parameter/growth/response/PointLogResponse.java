package com.laotang.parameter.growth.response;

/**
 * 积分流水。
 */
public record PointLogResponse(
        String id,
        Integer changeAmount,
        Integer balance,
        String source,
        String bizId,
        String remark,
        String createdAt
) {
}
