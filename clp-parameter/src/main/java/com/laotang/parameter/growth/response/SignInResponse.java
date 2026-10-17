package com.laotang.parameter.growth.response;

/**
 * 签到结果。
 */
public record SignInResponse(
        boolean signed,
        int continuousDays,
        int pointsAwarded,
        int balance
) {
}
