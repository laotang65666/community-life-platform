package com.laotang.core.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记收藏,对应 tb_blog_favorite。
 */
@Data
@TableName("tb_blog_favorite")
public class BlogFavorite {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long blogId;

    private Long userId;

    private LocalDateTime createdAt;
}
