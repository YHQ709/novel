package io.github.xxyopen.novel.service;

import io.github.xxyopen.novel.core.common.req.PageReqDto;
import io.github.xxyopen.novel.core.common.resp.PageRespDto;
import io.github.xxyopen.novel.core.common.resp.RestResp;
import io.github.xxyopen.novel.dto.req.*;
import io.github.xxyopen.novel.dto.resp.*;


/**
 * 会员模块 服务类
 *
 * @author xiongxiaoyang
 * @date 2022/5/17
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param dto 注册参数
     * @return JWT
     */
    RestResp<UserRegisterRespDto> register(UserRegisterReqDto dto);

    /**
     * 用户登录
     *
     * @param dto 登录参数
     * @return JWT + 昵称
     */
    RestResp<UserLoginRespDto> login(UserLoginReqDto dto);

    /**
     * 用户反馈
     *
     * @param userId  反馈用户ID
     * @param content 反馈内容
     * @return void
     */
    RestResp<Void> saveFeedback(Long userId, String content);

    /**
     * 分页查询反馈
     *
     * @param userId     反馈用户ID
     * @param pageReqDto 分页参数
     * @return 反馈分页列表数据
     */
    RestResp<PageRespDto<UserFeedbackRespDto>> listFeedbacks(Long userId, PageReqDto pageReqDto);

    /**
     * 用户信息修改
     *
     * @param dto 用户信息
     * @return void
     */
    RestResp<Void> updateUserInfo(UserInfoUptReqDto dto);

    /**
     * 用户反馈删除
     *
     * @param userId 用户ID
     * @param id     反馈ID
     * @return void
     */
    RestResp<Void> deleteFeedback(Long userId, Long id);

    /**
     * 查询书架状态接口
     *
     * @param userId 用户ID
     * @param bookId 小说ID
     * @return 0-不在书架 1-已在书架
     */
    RestResp<Integer> getBookshelfStatus(Long userId, String bookId);

    /**
     * 用户信息查询
     * @param userId 用户ID
     * @return 用户信息
     */
    RestResp<UserInfoRespDto> getUserInfo(Long userId);

    /**
     * 分页查询用户书架列表
     * @param userId 用户ID
     * @return 用户书架分页列表数据
     */
    RestResp<PageRespDto<UserBookshelfRespDto>> listBookshelf(Long userId, PageReqDto pageReqDto);

    /**
     * 加入书架
     *
     * @param dto 书架相关 DTO
     * @return void
     */
    RestResp<Void> addBookshelf(UserBookshelfReqDto dto);

    /**
     * 从书架移除
     *
     * @param userId 用户ID
     * @param bookId 小说ID
     * @return void
     */
    RestResp<Void> deleteBookshelf(Long userId, String bookId);

    /**
     * 修改密码
     *
     * @param dto 密码相关dto
     * @return void
     */
    RestResp<Void> setPassword(UserPasswordReqDto dto);

    /**
     * 添加阅读历史
     *
     * @param dto 阅读历史相关 DTO
     * @return void
     */
    RestResp<Void> addReadHistory(UserReadHistoryReqDto dto);

    /**
     * 分页查询充值记录
     *
     * @param userId     充值用户ID
     * @param pageReqDto 分页参数
     * @return 充值分页列表数据
     */
    RestResp<PageRespDto<UserPayLogRespDto>> listPayLogs(Long userId, PageReqDto pageReqDto);

    /**
     * 分页查询消费记录
     *
     * @param userId     消费用户ID
     * @param pageReqDto 分页参数
     * @return 消费分页列表数据
     */
    RestResp<PageRespDto<UserConsumeLogRespDto>> listConsumeLogs(Long userId, PageReqDto pageReqDto);
}
