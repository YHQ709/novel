package io.github.xxyopen.novel.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.xxyopen.novel.core.json.serializer.UsernameSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小说评论详情 响应DTO
 * @author YHQ
 * @date 2026/2/15
 */
@Data
@Builder
public class BookCommentDetailRespDto {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "评论内容")
    private String commentContent;

    @Schema(description = "回复数量")
    private Integer replyCount;

    @Schema(description = "评论用户")
    @JsonSerialize(using = UsernameSerializer.class)
    private String commentUser;

    @Schema(description = "评论用户ID")
    private Long commentUserId;

    @Schema(description = "评论用户头像")
    private String commentUserPhoto;

    @Schema(description = "评论时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime commentTime;

}
