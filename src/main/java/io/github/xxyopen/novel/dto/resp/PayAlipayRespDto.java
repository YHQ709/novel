package io.github.xxyopen.novel.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 支付宝支付 响应DTO
 *
 * @author YHQ
 * @date 2026/2/23
 */
@Data
@Builder
public class PayAlipayRespDto {

    /**
     * 支付表单（用于前端自动提交）
     */
    @Schema(description = "支付表单HTML")
    private String payForm;

    /**
     * 商户订单号
     */
    @Schema(description = "商户订单号")
    private String outTradeNo;

    /**
     * 支付金额（分）
     */
    @Schema(description = "支付金额（分）")
    private Integer totalAmount;

    /**
     * 支付二维码地址（可选，用于扫码支付）
     */
    @Schema(description = "支付二维码地址")
    private String qrCode;
}