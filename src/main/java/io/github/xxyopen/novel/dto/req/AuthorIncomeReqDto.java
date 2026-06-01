package io.github.xxyopen.novel.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 作家收入更新 请求DTO
 *
 * @author YHQ
 * @date 2026/2/24
 */
@Data
@Builder
public class AuthorIncomeReqDto implements Serializable {


    private static final long serialVersionUID = 1L;

    /**
     * 作家ID
     */
    @Schema(description = "作家ID", required = true)
    private Long authorId;

    /**
     * 小说ID
     */
    @Schema(description = "小说ID", required = true)
    private Long bookId;

    /**
     * 用户支付金额（单位：分）
     */
    @Schema(description = "用户支付金额", required = true)
    private Integer userPayAmount;

    /**
     * 收入日期
     */
    @Schema(description = "收入日期", required = true)
    private LocalDate incomeDate;

    /**
     * 用户ID（购买者）
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 章节ID
     */
    @Schema(description = "章节ID")
    private Long chapterId;
}
