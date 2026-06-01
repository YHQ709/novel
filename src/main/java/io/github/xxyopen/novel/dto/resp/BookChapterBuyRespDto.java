package io.github.xxyopen.novel.dto.resp;

import lombok.Builder;
import lombok.Data;

/**
 * 小说VIP章节购买 响应DTO
 *
 * @author YHQ
 * @date 2026/2/24
 */
@Data
@Builder
public class BookChapterBuyRespDto {

    /**
     * 购买后账户余额
     */
    private Long accountBalance;

    /**
     * 章节内容（购买成功后才返回）
     */
    private String chapterContent;

    /**
     * 章节名称
     */
    private String chapterName;
}
