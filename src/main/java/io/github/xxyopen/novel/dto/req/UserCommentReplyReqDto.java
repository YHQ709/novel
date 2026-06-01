package io.github.xxyopen.novel.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 用户发表评论回复 请求DTO
 * @author YHQ
 * @date 2026/2/15
 */
@Data
public class UserCommentReplyReqDto {

    private Long userId;

    @Schema(description = "评论ID", required = true)
    @NotNull(message="评论ID不能为空！")
    private Long commentId;

    @Schema(description = "回复内容", required = true)
    @NotBlank(message="回复不能为空！")
    @Length(min = 10,max = 512)
    private String replyContent;
}
