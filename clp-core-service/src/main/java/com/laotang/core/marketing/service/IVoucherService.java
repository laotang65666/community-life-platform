package com.laotang.core.marketing.service;

import com.laotang.common.result.PageResult;
import com.laotang.parameter.marketing.request.CreateVoucherRequest;
import com.laotang.parameter.marketing.response.VoucherBriefResponse;
import com.laotang.parameter.marketing.response.VoucherResponse;

/**
 * 优惠券服务。
 */
public interface IVoucherService {

    VoucherResponse create(Long merchantId, CreateVoucherRequest request);

    PageResult<VoucherBriefResponse> list(Long shopId, long page, long size);

    VoucherResponse getDetail(Long voucherId);
}
