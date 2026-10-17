package com.laotang.core.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.content.entity.BlogFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 笔记收藏数据访问。
 */
@Mapper
public interface BlogFavoriteMapper extends BaseMapper<BlogFavorite> {
}
