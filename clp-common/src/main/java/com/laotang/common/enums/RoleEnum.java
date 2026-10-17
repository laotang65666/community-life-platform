package com.laotang.common.enums;

/**
 * 角色。等级越高权限越大:MERCHANT 天然包含 USER 权限。
 */
public enum RoleEnum {

    USER(1),
    MERCHANT(2),
    ADMIN(3);

    private final int level;

    RoleEnum(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * 当前角色是否满足所需的最低角色。
     */
    public boolean hasAtLeast(RoleEnum required) {
        return this.level >= required.level;
    }

    public static RoleEnum of(String value) {
        if (value != null) {
            for (RoleEnum role : values()) {
                if (role.name().equalsIgnoreCase(value)) {
                    return role;
                }
            }
        }
        throw new IllegalArgumentException("未知角色: " + value);
    }
}
