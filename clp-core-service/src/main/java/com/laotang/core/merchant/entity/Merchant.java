package com.laotang.core.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户入驻申请,对应 tb_merchant。
 */
@Data
@TableName("tb_merchant")
public class Merchant {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String shopName;

    private String licenseNo;

    private String address;

    private String contactName;

    private String contactPhone;

    /**
     * PENDING / APPROVED / REJECTED
     */
    private String auditStatus;

    private String auditRemark;

    private Long auditedBy;

    private LocalDateTime auditedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
