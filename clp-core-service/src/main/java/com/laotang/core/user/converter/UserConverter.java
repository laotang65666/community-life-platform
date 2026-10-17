package com.laotang.core.user.converter;

import com.laotang.core.user.entity.User;
import com.laotang.parameter.user.response.MyProfileResponse;
import com.laotang.parameter.user.response.UserBriefResponse;
import com.laotang.parameter.user.response.UserProfileResponse;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * User 实体到 DTO 的转换。
 *
 * <p>ID 统一转字符串,避免前端 JS 精度丢失。</p>
 */
public final class UserConverter {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private UserConverter() {
    }

    public static UserProfileResponse toProfile(User user,
                                                long followerCount,
                                                long followingCount,
                                                boolean following) {
        return new UserProfileResponse(
                String.valueOf(user.getId()),
                user.getNickname(),
                user.getAvatar(),
                user.getIntro(),
                user.getRole(),
                followerCount,
                followingCount,
                following,
                format(user.getCreatedAt()));
    }

    public static MyProfileResponse toMyProfile(User user, long followerCount, long followingCount) {
        return new MyProfileResponse(
                String.valueOf(user.getId()),
                user.getPhone(),
                user.getNickname(),
                user.getAvatar(),
                user.getIntro(),
                user.getRole(),
                followerCount,
                followingCount,
                format(user.getCreatedAt()));
    }

    public static UserBriefResponse toBrief(User user) {
        return new UserBriefResponse(
                String.valueOf(user.getId()),
                user.getNickname(),
                user.getAvatar());
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).format(FORMATTER);
    }
}
