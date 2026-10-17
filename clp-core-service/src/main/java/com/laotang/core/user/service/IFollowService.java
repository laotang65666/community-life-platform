package com.laotang.core.user.service;

import com.laotang.common.result.PageResult;
import com.laotang.parameter.user.response.UserBriefResponse;

/**
 * 关注关系服务。
 */
public interface IFollowService {

    void follow(Long userId, Long targetUserId);

    void unfollow(Long userId, Long targetUserId);

    PageResult<UserBriefResponse> listFollowers(Long userId, long page, long size);

    PageResult<UserBriefResponse> listFollowing(Long userId, long page, long size);
}
