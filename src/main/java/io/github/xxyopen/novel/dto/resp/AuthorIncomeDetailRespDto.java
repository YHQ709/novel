package io.github.xxyopen.novel.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 稿费收入明细 响应DTO
 *
 * @author YHQ
 * @date 2026/2/22
 */
@Data
@Builder
public class AuthorIncomeDetailRespDto {

    /**
     * 收入ID
     */
    @Schema(description = "收入ID")
    private Long id;

    /**
     * 收入日期
     */
    private LocalDate incomeDate;

    /**
     * 订阅总额
     */
    private Integer incomeAccount;

    /**
     * 订阅次数
     */
    private Integer incomeCount;

    /**
     * 订阅人数
     */
    private Integer incomeNumber;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
