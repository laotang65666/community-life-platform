package com.laotang.parameter.content.response;

import java.util.List;

/**
 * 关注流滚动分页结果。
 *
 * @param lastId 本次返回的最后一条的时间戳,下次请求原样带上
 * @param offset 与 lastId 同分的已消费条数
 */
public record FeedResponse(
        List<BlogBriefResponse> list,
        long lastId,
        int offset
) {
}
