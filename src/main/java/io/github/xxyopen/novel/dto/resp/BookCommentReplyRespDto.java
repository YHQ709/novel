package io.github.xxyopen.novel.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.xxyopen.novel.core.json.serializer.UsernameSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小说评论回复 响应DTO
 * @author YHQ
 * @date 2026/2/15
 */
@Data
@Builder
public class BookCommentReplyRespDto {

    @Schema(description = "回复总数")
    private Long replyTotal;

    @Schema(description = "回复列表")
    private List<CommentReplyInfo> replies;

    @Data
    @Builder
    public static class CommentReplyInfo {

        @Schema(description = "回复ID")
        private Long id;

        @Schema(description = "回复内容")
        private String replyContent;

        @Schema(description = "回复用户")
        @JsonSerialize(using = UsernameSerializer.class)
        private String replyUser;

        @Schema(description = "回复用户ID")
        private Long replyUserId;

        @Schema(description = "回复用户头像")
        private String replyUserPhoto;

        @Schema(description = "回复时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime replyTime;

    }

}
