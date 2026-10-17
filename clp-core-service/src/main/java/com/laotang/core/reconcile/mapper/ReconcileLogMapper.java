package com.laotang.core.reconcile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laotang.core.reconcile.entity.ReconcileLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账日志数据访问。
 */
@Mapper
public interface ReconcileLogMapper extends BaseMapper<ReconcileLog> {
}
