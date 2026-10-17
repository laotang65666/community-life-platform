package com.laotang.core.user.service;

import com.laotang.core.user.entity.User;
import com.laotang.parameter.user.request.UpdateProfileRequest;
import com.laotang.parameter.user.response.MyProfileResponse;
import com.laotang.parameter.user.response.UserProfileResponse;

import java.util.Collection;
import java.util.List;

/**
 * 账号服务。其他业务域通过这个接口访问账号,不要直接注入 UserMapper。
 */
public interface IUserService {

    User findByPhone(String phone);

    User createByPhone(String phone);

    void updateLastLogin(Long userId);

    void updateRole(Long userId, String role);

    User getById(Long userId);

    List<User> findByIds(Collection<Long> ids);

    /**
     * 查看某个用户的公开资料。
     *
     * @param targetUserId  被查看的用户
     * @param currentUserId 当前登录用户,用于判断是否已关注
     */
    UserProfileResponse getProfile(Long targetUserId, Long currentUserId);

    /**
     * 当前登录用户的完整资料。
     */
    MyProfileResponse getMyProfile(Long userId);

    /**
     * 修改资料。只更新非 null 字段。
     */
    void updateProfile(Long userId, UpdateProfileRequest request);
}
