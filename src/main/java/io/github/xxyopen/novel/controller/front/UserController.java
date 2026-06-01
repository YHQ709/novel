package io.github.xxyopen.novel.controller.front;

import io.github.xxyopen.novel.core.auth.UserHolder;
import io.github.xxyopen.novel.core.common.req.PageReqDto;
import io.github.xxyopen.novel.core.common.resp.PageRespDto;
import io.github.xxyopen.novel.core.common.resp.RestResp;
import io.github.xxyopen.novel.core.constant.ApiRouterConsts;
import io.github.xxyopen.novel.core.constant.SystemConfigConsts;
import io.github.xxyopen.novel.dto.req.*;
import io.github.xxyopen.novel.dto.resp.*;
import io.github.xxyopen.novel.service.BookService;
import io.github.xxyopen.novel.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前台门户-会员模块 API 控制器
 *
 * @author xiongxiaoyang
 * @date 2022/5/17
 */
@Tag(name = "UserController", description = "前台门户-会员模块")
@SecurityRequirement(name = SystemConfigConsts.HTTP_AUTH_HEADER_NAME)
@RestController
@RequestMapping(ApiRouterConsts.API_FRONT_USER_URL_PREFIX)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final BookService bookService;

    /**
     * 用户注册接口
     */
    @Operation(summary = "用户注册接口")
    @PostMapping("register")
    public RestResp<UserRegisterRespDto> register(@Valid @RequestBody UserRegisterReqDto dto) {
        return userService.register(dto);
    }

    /**
     * 用户登录接口
     */
    @Operation(summary = "用户登录接口")
    @PostMapping("login")
    public RestResp<UserLoginRespDto> login(@Valid @RequestBody UserLoginReqDto dto) {
        return userService.login(dto);
    }

    /**
     * 用户信息查询接口
     */
    @Operation(summary = "用户信息查询接口")
    @GetMapping
    public RestResp<UserInfoRespDto> getUserInfo() {
        return userService.getUserInfo(UserHolder.getUserId());
    }

    /**
     * 用户信息修改接口
     */
    @Operation(summary = "用户信息修改接口")
    @PutMapping
    public RestResp<Void> updateUserInfo(@Valid @RequestBody UserInfoUptReqDto dto) {
        dto.setUserId(UserHolder.getUserId());
        return userService.updateUserInfo(dto);
    }


    /**
     * 分页查询反馈
     */
    @Operation(summary = "查询用户反馈列表接口")
    @GetMapping("feedbacks")
    public RestResp<PageRespDto<UserFeedbackRespDto>> listFeedbacks(PageReqDto dto) {
        return userService.listFeedbacks(UserHolder.getUserId(), dto);
    }

    /**
     * 用户反馈提交接口
     */
    @Operation(summary = "用户反馈提交接口")
    @PostMapping("feedback")
    public RestResp<Void> submitFeedback(@RequestBody String content) {
        return userService.saveFeedback(UserHolder.getUserId(), content);
    }

    /**
     * 用户反馈删除接口
     */
    @Operation(summary = "用户反馈删除接口")
    @DeleteMapping("feedback/{id}")
    public RestResp<Void> deleteFeedback(@Parameter(description = "反馈ID") @PathVariable Long id) {
        return userService.deleteFeedback(UserHolder.getUserId(), id);
    }

    /**
     * 发表评论接口
     */
    @Operation(summary = "发表评论接口")
    @PostMapping("comment")
    public RestResp<Void> comment(@Valid @RequestBody UserCommentReqDto dto) {
        dto.setUserId(UserHolder.getUserId());
        return bookService.saveComment(dto);
    }

    /**
     * 修改评论接口
     */
    @Operation(summary = "修改评论接口")
    @PutMapping("comment/{id}")
    public RestResp<Void> updateComment(@Parameter(description = "评论ID") @PathVariable Long id,
        String content) {
        return bookService.updateComment(UserHolder.getUserId(), id, content);
    }

    /**
     * 删除评论接口
     */
    @Operation(summary = "删除评论接口")
    @DeleteMapping("comment/{id}")
    public RestResp<Void> deleteComment(@Parameter(description = "评论ID") @PathVariable Long id) {
        return bookService.deleteComment(UserHolder.getUserId(), id);
    }

    /**
     * 分页查询书架列表接口
     */
    @Operation(summary = "书架列表查询接口")
    @GetMapping("bookshelf")
    public RestResp<PageRespDto<UserBookshelfRespDto>> listBookshelf(PageReqDto pageReqDto) {
        return userService.listBookshelf(UserHolder.getUserId(), pageReqDto);
    }

    /**
     * 添加书架接口
     */
    @Operation(summary = "添加书架接口")
    @PostMapping("bookshelf")
    public RestResp<Void> addBookshelf(@Valid @RequestBody UserBookshelfReqDto dto) {
        return userService.addBookshelf(dto);
    }

    /**
     * 移除书架接口
     */
    @Operation(summary = "移除书架接口")
    @DeleteMapping("bookshelf/{bookId}")
    public RestResp<Void> deleteBookshelf(@Parameter(description = "小说ID") @PathVariable String bookId) {
        return userService.deleteBookshelf(UserHolder.getUserId(), bookId);
    }

    /**
     * 查询书架状态接口 0-不在书架 1-已在书架
     */
    @Operation(summary = "查询书架状态接口")
    @GetMapping("bookshelf_status")
    public RestResp<Integer> getBookshelfStatus(@Parameter(description = "小说ID") String bookId) {
        return userService.getBookshelfStatus(UserHolder.getUserId(), bookId);
    }

    /**
     * 分页查询评论
     */
    @Operation(summary = "查询会员评论列表接口")
    @GetMapping("comments")
    public RestResp<PageRespDto<UserCommentRespDto>> listComments(PageReqDto pageReqDto) {
        return bookService.listComments(UserHolder.getUserId(), pageReqDto);
    }

    /**
     * 修改密码接口
     */
    @Operation(summary = "修改密码接口")
    @PostMapping("password")
    public RestResp<Void> setPassword(@Valid @RequestBody UserPasswordReqDto dto) {
        return userService.setPassword(dto);
    }

    /**
     * 发表评论回复接口
     */
    @Operation(summary = "发表评论回复接口")
    @PostMapping("comment/reply")
    public RestResp<Void> commentReply(@Valid @RequestBody UserCommentReplyReqDto dto) {
        dto.setUserId(UserHolder.getUserId());
        return bookService.saveCommentReply(dto);
    }

    /**
     * 添加阅读历史接口
     */
    @Operation(summary = "添加阅读历史接口")
    @PostMapping("addReadHistory")
    public RestResp<Void> addReadHistory(@Valid @RequestBody UserReadHistoryReqDto dto) {
        return userService.addReadHistory(dto);
    }

    /**
     * 分页查询充值记录
     */
    @Operation(summary = "查询用户充值记录列表接口")
    @GetMapping("payLogs")
    public RestResp<PageRespDto<UserPayLogRespDto>> listPayLogs(PageReqDto dto) {
        return userService.listPayLogs(UserHolder.getUserId(), dto);
    }

    /**
     * 分页查询反馈
     */
    @Operation(summary = "查询用户消费记录列表接口")
    @GetMapping("consumeLogs")
    public RestResp<PageRespDto<UserConsumeLogRespDto>> listConsumeLogs(PageReqDto dto) {
        return userService.listConsumeLogs(UserHolder.getUserId(), dto);
    }

}
