package com.laotang.parameter.content.response;

import java.util.List;

/**
 * 笔记详情。
 */
public record BlogResponse(
        String id,
        String userId,
        String authorNickname,
        String authorAvatar,
        String shopId,
        String title,
        String content,
        List<String> images,
        int likedCount,
        int commentCount,
        boolean liked,
        boolean favorited,
        String createdAt
) {
}
