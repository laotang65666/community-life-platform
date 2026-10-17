package com.laotang.core.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.content.entity.Blog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 笔记数据访问。
 */
@Mapper
public interface BlogMapper extends BaseMapper<Blog> {

    @Update("UPDATE tb_blog SET liked_count = liked_count + 1 WHERE id = #{id}")
    int incrementLikedCount(@Param("id") Long id);

    @Update("UPDATE tb_blog SET liked_count = liked_count - 1 WHERE id = #{id} AND liked_count > 0")
    int decrementLikedCount(@Param("id") Long id);

    @Update("UPDATE tb_blog SET comment_count = comment_count + 1 WHERE id = #{id}")
    int incrementCommentCount(@Param("id") Long id);

    @Update("UPDATE tb_blog SET comment_count = comment_count - 1 WHERE id = #{id} AND comment_count > 0")
    int decrementCommentCount(@Param("id") Long id);
}
