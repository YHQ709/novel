package io.github.xxyopen.novel.service;

import io.github.xxyopen.novel.core.common.req.PageReqDto;
import io.github.xxyopen.novel.core.common.resp.PageRespDto;
import io.github.xxyopen.novel.core.common.resp.RestResp;
import io.github.xxyopen.novel.dto.req.AuthorIncomeDetailReqDto;
import io.github.xxyopen.novel.dto.req.AuthorIncomeReqDto;
import io.github.xxyopen.novel.dto.req.AuthorRegisterReqDto;
import io.github.xxyopen.novel.dto.resp.AuthorIncomeDetailRespDto;
import io.github.xxyopen.novel.dto.resp.AuthorIncomeRespDto;
import io.github.xxyopen.novel.dto.resp.BookChapterRespDto;

import java.util.concurrent.CompletableFuture;

/**
 * 作家模块 业务服务类
 *
 * @author xiongxiaoyang
 * @date 2022/5/23
 */
public interface AuthorService {

    /**
     * 作家注册
     *
     * @param dto 注册参数
     * @return void
     */
    RestResp<Void> register(AuthorRegisterReqDto dto);

    /**
     * 查询作家状态
     *
     * @param userId 用户ID
     * @return 作家状态
     */
    RestResp<Integer> getStatus(Long userId);

    /**
     * 查询作家订阅明细列表
     *
     * @param dto    分页请求参数
     * @return 章节分页列表数据
     */
    RestResp<PageRespDto<AuthorIncomeDetailRespDto>> listAuthorSubscribeDetails(PageReqDto dto);

    /**
     * 查询作家稿费汇总列表
     *
     * @param dto    分页请求参数
     * @return 章节分页列表数据
     */
    RestResp<PageRespDto<AuthorIncomeRespDto>> listAuthorIncomes(PageReqDto dto);

    /**
     * 异步更新作家收入（主方法）
     * 在用户购买VIP章节后调用
     *
     * @param dto 收入更新请求DTO
     * @return void
     */
    CompletableFuture<RestResp<Void>> updateAuthorIncomeAsync(AuthorIncomeReqDto dto);

    /**
     * 更新作家收入明细
     * 可以直接调用此方法更新日明细
     *
     * @param dto 收入明细更新请求DTO
     * @return void
     */
    RestResp<Void> updateAuthorIncomeDetail(AuthorIncomeDetailReqDto dto);
}
