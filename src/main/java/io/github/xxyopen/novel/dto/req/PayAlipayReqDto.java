package io.github.xxyopen.novel.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 支付宝支付 请求DTO
 *
 * @author YHQ
 * @date 2026/2/23
 */
@Data
public class PayAlipayReqDto {

    /**
     * 充值金额（元）
     */
    @Schema(description = "充值金额（元）", required = true)
    @NotNull
    private Integer amount;

    /**
     * 支付方式;1-支付宝 2-微信
     */
    @Schema(description = "支付方式;1-支付宝 2-微信", required = true)
    @NotNull
    private Integer payChannel;

    /**
     * 商品类型;0-星币 1-包年VIP
     */
    @Schema(description = "商品类型;0-星币 1-包年VIP")
    private Integer productType;

    /**
     * 商品ID
     */
    @Schema(description = "商品ID")
    private Long productId;

    /**
     * 商品名称
     */
    @Schema(description = "商品名称")
    private String productName;

    /**
     * 商品值（星币数量）
     */
    @Schema(description = "商品值（星币数量）")
    private Integer productValue;
}