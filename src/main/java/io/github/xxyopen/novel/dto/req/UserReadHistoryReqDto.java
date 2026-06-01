package io.github.xxyopen.novel.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户添加阅读历史 请求DTO
 *
 * @author YHQ
 * @date 2026/2/20
 */
@Data
public class UserReadHistoryReqDto {

    /**
     * 小说ID
     */
    @Schema(description = "小说ID", required = true)
    @NotNull
    private Long bookId;

    /**
     * 上一次阅读章节ID
     * */
    @Schema(description = "上一次阅读章节ID")
    private Long preContentId;
}
