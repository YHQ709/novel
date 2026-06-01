package io.github.xxyopen.novel.dto.resp;

import lombok.Builder;
import lombok.Data;

/**
 * 用户消费记录 响应DTO
 *
 * @author YHQ
 * @date 2026/3/12
 */
@Data
@Builder
public class UserConsumeLogRespDto {

    /**
     * 消费用户ID
     */
    private Long userId;

    /**
     * 消费使用的金额;单位：星币
     */
    private Integer amount;

    /**
     * 消费商品类型;0-小说VIP章节
     */
    private Integer productType;

    /**
     * 消费的的商品ID;例如：章节ID
     */
    private Long productId;

    /**
     * 消费的的商品名;例如：章节名
     */
    private String producName;

    /**
     * 消费的的商品值;例如：1
     */
    private Integer producValue;
}
