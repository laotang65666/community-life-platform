package com.laotang.core.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号实体,对应 tb_user。
 */
@Data
@TableName("tb_user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String phone;

    private String password;

    private String nickname;

    private String avatar;

    private String intro;

    private String role;

    private String status;

    private LocalDateTime lastLoginAt;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
