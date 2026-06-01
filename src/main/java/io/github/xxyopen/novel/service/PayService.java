package io.github.xxyopen.novel.service;

import io.github.xxyopen.novel.core.common.resp.RestResp;
import io.github.xxyopen.novel.dto.req.PayAlipayReqDto;
import io.github.xxyopen.novel.dto.resp.PayAlipayRespDto;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 支付模块 服务类
 *
 * @author YHQ
 * @date 2026/2/23
 */
public interface PayService {

    /**
     * 创建支付宝支付
     */
    RestResp<PayAlipayRespDto> createAlipay(PayAlipayReqDto reqDto);

    /**
     * 支付宝异步通知
     */
    String alipayNotify(HttpServletRequest request);

    /**
     * 支付宝同步通知
     */
    String alipayReturn(HttpServletRequest request);
}
