package com.laotang.parameter.content.response;

import java.util.List;

/**
 * 笔记列表项。
 */
public record BlogBriefResponse(
        String id,
        String userId,
        String authorNickname,
        String authorAvatar,
        String title,
        List<String> images,
        int likedCount,
        int commentCount,
        String createdAt
) {
}
