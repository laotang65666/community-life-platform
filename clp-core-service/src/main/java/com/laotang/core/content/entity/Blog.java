package com.laotang.core.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记,对应 tb_blog。
 */
@Data
@TableName(value = "tb_blog", autoResultMap = true)
public class Blog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long shopId;

    private String title;

    private String content;

    /**
     * 图片 URL 列表,数据库为 JSON。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private Integer likedCount;

    private Integer commentCount;

    /**
     * PUBLISHED / HIDDEN / DELETED
     */
    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
