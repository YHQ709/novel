package io.github.xxyopen.novel.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户书架 响应DTO
 *
 * @author YHQ
 * @date 2026/2/8
 */
@Data
@Builder
public class UserBookshelfRespDto {

    /**
     * 用户ID
     * */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 小说ID
     * */
    @Schema(description = "小说ID")
    private String bookId;

    /**
     * 上一次阅读章节ID
     * */
    @Schema(description = "上一次阅读章节ID")
    private Long preContentId;

    /**
     * 作品方向;0-男频 1-女频
     */
    private Integer workDirection;

    /**
     * 类别ID
     */
    @Schema(description = "类别ID")
    private Long categoryId;

    /**
     * 类别名
     */
    @Schema(description = "类别名")
    private String categoryName;

    /**
     * 小说名
     */
    @Schema(description = "小说名")
    private String bookName;

    /**
     * 最新章节ID
     */
    private Long lastChapterId;

    /**
     * 最新章节名
     */
    @Schema(description = "最新章节名")
    private String lastChapterName;

    /**
     * 最新章节更新时间
     */
    @Schema(description = "最新章节更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime updateTime;
}
