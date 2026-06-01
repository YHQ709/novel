package io.github.xxyopen.novel.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 用户修改密码 请求DTO
 *
 * @author YHQ
 * @date 2026/2/11
 */
@Data
public class UserPasswordReqDto {
    /**
     * 原密码
     */
    @NotBlank
    @Schema(description = "原密码")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank
    @Schema(description = "新密码", minLength = 6, maxLength = 20)
    @Length(min = 6, max = 20)
    private String newPassword;

    /**
     * 确认密码
     */
    @NotBlank
    @Schema(description = "确认密码")
    private String confirmPassword;
}
