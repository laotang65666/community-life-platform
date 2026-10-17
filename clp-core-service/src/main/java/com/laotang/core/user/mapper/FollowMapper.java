package com.laotang.core.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.user.entity.Follow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 关注关系数据访问。
 */
@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
}
