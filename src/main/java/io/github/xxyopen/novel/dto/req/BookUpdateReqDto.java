package io.github.xxyopen.novel.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小说修改 请求DTO
 *
 * @author YHQ
 * @date 2026/2/22
 */
@Data
public class BookUpdateReqDto {

    /**
     * 类别ID
     */
    @Schema(description = "类别ID", required = true)
    @NotNull
    private Long categoryId;

    /**
     * 类别名
     */
    @Schema(description = "类别名", required = true)
    @NotBlank
    private String categoryName;

    /**
     * 小说封面地址
     */
    @Schema(description = "小说封面地址", required = true)
    @NotBlank
    private String picUrl;

    /**
     * 小说名
     */
    @Schema(description = "小说名", required = true)
    @NotBlank
    private String bookName;

    /**
     * 书籍描述
     */
    @Schema(description = "书籍描述", required = true)
    @NotBlank
    private String bookDesc;

    /**
     * 书籍状态;0-连载中 1-已完结
     */
    private Integer bookStatus;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
