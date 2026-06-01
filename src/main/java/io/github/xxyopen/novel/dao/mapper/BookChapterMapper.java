package io.github.xxyopen.novel.dao.mapper;

import io.github.xxyopen.novel.dao.entity.BookChapter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 小说章节 Mapper 接口
 * </p>
 *
 * @author xiongxiaoyang
 * @date 2022/05/11
 */
public interface BookChapterMapper extends BaseMapper<BookChapter> {

    /**
     * 获取小说的第一章ID
     * @param bookId 小说ID
     * @return 第一章ID，如果没有返回null
     */
    Long selectFirstChapterId(@Param("bookId") Long bookId);

}
