package io.github.xxyopen.novel.dao.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.xxyopen.novel.dao.entity.UserBookshelf;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.xxyopen.novel.dto.resp.UserBookshelfRespDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户书架 Mapper 接口
 * </p>
 *
 * @author xiongxiaoyang
 * @date 2022/05/11
 */
public interface UserBookshelfMapper extends BaseMapper<UserBookshelf> {

    /**
     * 根据用户ID查询书架
     *
     * @param userId 用户ID
     * @return 书架列表
     */
    List<UserBookshelfRespDto> selectUserBookshelf(IPage<UserBookshelfRespDto> page, @Param("userId") Long userId);
}
