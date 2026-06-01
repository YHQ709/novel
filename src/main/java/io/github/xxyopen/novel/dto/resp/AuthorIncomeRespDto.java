package io.github.xxyopen.novel.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
/**
 * 稿费汇总 响应DTO
 *
 * @author YHQ
 * @date 2026/2/22
 */
public class AuthorIncomeRespDto {

    /**
     * 收入ID
     */
    private Long id;

    /**
     * 收入月份
     */
    private LocalDate incomeMonth;

    /**
     * 税前收入;单位：分
     */
    private Integer preTaxIncome;

    /**
     * 税后收入;单位：分
     */
    private Integer afterTaxIncome;

    /**
     * 支付状态;0-待支付 1-已支付
     */
    private Integer payStatus;
}
