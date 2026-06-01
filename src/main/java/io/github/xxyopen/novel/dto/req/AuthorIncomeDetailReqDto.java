package io.github.xxyopen.novel.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 作家收入明细更新 请求DTO
 *
 * @author YHQ
 * @date 2026/2/24
 */
@Data
public class AuthorIncomeDetailReqDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 作家ID
     */
    @Schema(description = "作家ID", required = true)
    private Long authorId;

    /**
     * 小说ID（0表示全部作品）
     */
    @Schema(description = "小说ID", required = true)
    private Long bookId;

    /**
     * 收入日期
     */
    @Schema(description = "收入日期", required = true)
    private LocalDate incomeDate;

    /**
     * 订阅总额（单位：分）
     */
    @Schema(description = "订阅总额（单位：分）", required = true)
    private Integer incomeAccount;

    /**
     * 订阅次数
     */
    @Schema(description = "订阅次数", required = true)
    private Integer incomeCount;

    /**
     * 订阅人数
     */
    @Schema(description = "订阅人数", required = true)
    private Integer incomeNumber;

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

    /**
     * 章节名称
     */
    @Schema(description = "章节名称")
    private String chapterName;
}
