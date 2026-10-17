package com.laotang.core.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记评论,对应 tb_blog_comment。
 */
@Data
@TableName("tb_blog_comment")
public class BlogComment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long blogId;

    private Long userId;

    /**
     * 父评论 ID;null 表示一级评论。
     */
    private Long parentId;

    /**
     * 被回复用户 ID。
     */
    private Long replyToUserId;

    private String content;

    /**
     * VISIBLE / HIDDEN
     */
    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
}
