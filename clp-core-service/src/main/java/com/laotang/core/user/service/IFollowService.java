package com.laotang.core.user.service;

import com.laotang.common.result.PageResult;
import com.laotang.parameter.user.response.UserBriefResponse;

import java.util.List;

/**
 * 关注关系服务。
 */
public interface IFollowService {

    void follow(Long userId, Long targetUserId);

    void unfollow(Long userId, Long targetUserId);

    PageResult<UserBriefResponse> listFollowers(Long userId, long page, long size);

    PageResult<UserBriefResponse> listFollowing(Long userId, long page, long size);

    /**
     * 粉丝 ID 列表,用于发笔记时写 Feed 收件箱。
     */
    List<Long> findFollowerIds(Long userId);

    /**
     * 关注对象 ID 列表,用于推拉结合读取大 V 发件箱。
     */
    List<Long> findFolloweeIds(Long userId);

    /**
     * 粉丝数。优先读 Redis 计数器,未命中时回源数据库并回填。
     */
    long getFollowerCount(Long userId);
}
