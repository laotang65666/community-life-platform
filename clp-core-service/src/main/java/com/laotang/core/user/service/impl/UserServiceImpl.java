package com.laotang.core.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.user.converter.UserConverter;
import com.laotang.core.user.entity.Follow;
import com.laotang.core.user.entity.User;
import com.laotang.core.user.mapper.FollowMapper;
import com.laotang.core.user.mapper.UserMapper;
import com.laotang.core.user.service.IUserService;
import com.laotang.parameter.user.request.UpdateProfileRequest;
import com.laotang.parameter.user.response.MyProfileResponse;
import com.laotang.parameter.user.response.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 账号服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final FollowMapper followMapper;

    @Override
    public User findByPhone(String phone) {
        return lambdaQuery().eq(User::getPhone, phone).one();
    }

    @Override
    public User createByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setPassword("");
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setAvatar("");
        user.setIntro("");
        user.setRole(DEFAULT_ROLE);
        user.setStatus(STATUS_ACTIVE);

        try {
            save(user);
            return user;
        } catch (DuplicateKeyException e) {
            // 并发注册:唯一索引兜底,返回已存在的账号
            return findByPhone(phone);
        }
    }

    @Override
    public void updateLastLogin(Long userId) {
        lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getLastLoginAt, LocalDateTime.now())
                .update();
    }

    @Override
    public User getById(Long userId) {
        return baseMapper.selectById(userId);
    }

    @Override
    public List<User> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return baseMapper.selectBatchIds(ids);
    }

    @Override
    public UserProfileResponse getProfile(Long targetUserId, Long currentUserId) {
        User user = getById(targetUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        long followerCount = countFollowers(targetUserId);
        long followingCount = countFollowing(targetUserId);
        boolean following = currentUserId != null
                && !currentUserId.equals(targetUserId)
                && isFollowing(currentUserId, targetUserId);

        return UserConverter.toProfile(user, followerCount, followingCount, following);
    }

    @Override
    public MyProfileResponse getMyProfile(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return UserConverter.toMyProfile(user, countFollowers(userId), countFollowing(userId));
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId);

        boolean changed = false;
        if (request.nickname() != null) {
            wrapper.set(User::getNickname, request.nickname());
            changed = true;
        }
        if (request.avatar() != null) {
            wrapper.set(User::getAvatar, request.avatar());
            changed = true;
        }
        if (request.intro() != null) {
            wrapper.set(User::getIntro, request.intro());
            changed = true;
        }

        if (changed) {
            baseMapper.update(null, wrapper);
        }
    }

    private long countFollowers(Long userId) {
        Long count = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowUserId, userId));
        return count == null ? 0L : count;
    }

    private long countFollowing(Long userId) {
        Long count = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getUserId, userId));
        return count == null ? 0L : count;
    }

    private boolean isFollowing(Long userId, Long targetUserId) {
        Long count = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, targetUserId));
        return count != null && count > 0;
    }
}
