package io.github.xxyopen.novel.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户充值记录 响应DTO
 *
 * @author YHQ
 * @date 2026/3/12
 */
@Data
@Builder
public class UserPayLogRespDto {

    /**
     * 充值用户ID
     */
    private Long userId;

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 充值金额;单位：分
     */
    private Integer amount;

    /**
     * 充值商品类型;0-星币 1-包年VIP
     */
    private Integer productType;

    /**
     * 充值商品ID
     */
    private Long productId;

    /**
     * 充值商品名;示例值：星币
     */
    private String productName;

    /**
     * 充值商品值;示例值：255
     */
    private Integer productValue;

    /**
     * 充值时间
     */
    private LocalDateTime payTime;
}
