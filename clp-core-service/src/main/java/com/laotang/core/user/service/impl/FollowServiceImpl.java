package com.laotang.core.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.core.user.converter.UserConverter;
import com.laotang.core.user.entity.Follow;
import com.laotang.core.user.entity.User;
import com.laotang.core.user.mapper.FollowMapper;
import com.laotang.core.user.service.IFollowService;
import com.laotang.core.user.service.IUserService;
import com.laotang.parameter.user.response.UserBriefResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 关注关系服务实现。
 */
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements IFollowService {

    private final FollowMapper followMapper;
    private final IUserService userService;
    private final StringRedisTemplate redis;

    @Override
    public void follow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF);
        }
        if (userService.getById(targetUserId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        // 先确保粉丝计数器存在(未命中会回源数据库)
        getFollowerCount(targetUserId);

        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowUserId(targetUserId);

        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            // 幂等:重复关注视为成功,不重复计数
            return;
        }
        redis.opsForValue().increment(RedisKey.userFollowers(targetUserId));
    }

    @Override
    public void unfollow(Long userId, Long targetUserId) {
        getFollowerCount(targetUserId);

        int rows = followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, targetUserId));
        if (rows > 0) {
            Long remaining = redis.opsForValue().decrement(RedisKey.userFollowers(targetUserId));
            if (remaining != null && remaining < 0) {
                redis.opsForValue().set(RedisKey.userFollowers(targetUserId), "0");
            }
        }
    }

    @Override
    public PageResult<UserBriefResponse> listFollowers(Long userId, long page, long size) {
        Page<Follow> result = followMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowUserId, userId)
                        .orderByDesc(Follow::getCreatedAt));
        return toBriefPage(result, Follow::getUserId);
    }

    @Override
    public PageResult<UserBriefResponse> listFollowing(Long userId, long page, long size) {
        Page<Follow> result = followMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getUserId, userId)
                        .orderByDesc(Follow::getCreatedAt));
        return toBriefPage(result, Follow::getFollowUserId);
    }

    @Override
    public List<Long> findFollowerIds(Long userId) {
        return followMapper.selectList(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowUserId, userId)
                        .select(Follow::getUserId))
                .stream()
                .map(Follow::getUserId)
                .toList();
    }

    @Override
    public List<Long> findFolloweeIds(Long userId) {
        return followMapper.selectList(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getUserId, userId)
                        .select(Follow::getFollowUserId))
                .stream()
                .map(Follow::getFollowUserId)
                .toList();
    }

    @Override
    public long getFollowerCount(Long userId) {
        String key = RedisKey.userFollowers(userId);
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            return Long.parseLong(cached);
        }

        Long count = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowUserId, userId));
        long value = count == null ? 0L : count;
        redis.opsForValue().set(key, String.valueOf(value));
        return value;
    }

    /**
     * 从关注分页结果中取出用户 ID,批量查询用户名片后按原顺序组装。
     */
    private PageResult<UserBriefResponse> toBriefPage(Page<Follow> result,
                                                      Function<Follow, Long> idExtractor) {
        List<Long> ids = result.getRecords().stream()
                .map(idExtractor)
                .toList();

        if (ids.isEmpty()) {
            return PageResult.of(List.of(), result.getCurrent(), result.getSize(), result.getTotal());
        }

        Map<Long, User> userMap = userService.findByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<UserBriefResponse> list = ids.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(UserConverter::toBrief)
                .toList();

        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }
}
