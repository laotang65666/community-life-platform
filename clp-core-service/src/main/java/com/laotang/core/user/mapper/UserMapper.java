package com.laotang.core.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账号数据访问。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
