package io.github.xxyopen.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.xxyopen.novel.core.auth.UserHolder;
import io.github.xxyopen.novel.core.common.constant.CommonConsts;
import io.github.xxyopen.novel.core.common.constant.ErrorCodeEnum;
import io.github.xxyopen.novel.core.common.exception.BusinessException;
import io.github.xxyopen.novel.core.common.req.PageReqDto;
import io.github.xxyopen.novel.core.common.resp.PageRespDto;
import io.github.xxyopen.novel.core.common.resp.RestResp;
import io.github.xxyopen.novel.core.constant.DatabaseConsts;
import io.github.xxyopen.novel.core.constant.SystemConfigConsts;
import io.github.xxyopen.novel.core.util.JwtUtils;
import io.github.xxyopen.novel.dao.entity.*;
import io.github.xxyopen.novel.dao.mapper.*;
import io.github.xxyopen.novel.dto.req.*;
import io.github.xxyopen.novel.dto.resp.*;
import io.github.xxyopen.novel.manager.redis.VerifyCodeManager;
import io.github.xxyopen.novel.service.UserService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

/**
 * 会员模块 服务实现类
 *
 * @author xiongxiaoyang
 * @date 2022/5/17
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserInfoMapper userInfoMapper;

    private final VerifyCodeManager verifyCodeManager;

    private final UserFeedbackMapper userFeedbackMapper;

    private final UserBookshelfMapper userBookshelfMapper;

    private final JwtUtils jwtUtils;

    private final BookChapterMapper bookChapterMapper;

    private final BookInfoMapper bookInfoMapper;

    private final UserReadHistoryMapper userReadHistoryMapper;

    private final UserPayLogMapper userPayLogMapper;
    private final UserConsumeLogMapper userConsumeLogMapper;

    @Override
    public RestResp<UserRegisterRespDto> register(UserRegisterReqDto dto) {
        // 校验图形验证码是否正确
        if (!verifyCodeManager.imgVerifyCodeOk(dto.getSessionId(), dto.getVelCode())) {
            // 图形验证码校验失败
            throw new BusinessException(ErrorCodeEnum.USER_VERIFY_CODE_ERROR);
        }

        // 校验手机号是否已注册
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.UserInfoTable.COLUMN_USERNAME, dto.getUsername())
            .last(DatabaseConsts.SqlEnum.LIMIT_1.getSql());
        if (userInfoMapper.selectCount(queryWrapper) > 0) {
            // 手机号已注册
            throw new BusinessException(ErrorCodeEnum.USER_NAME_EXIST);
        }

        // 注册成功，保存用户信息
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(
            DigestUtils.md5DigestAsHex(dto.getPassword().getBytes(StandardCharsets.UTF_8)));
        userInfo.setUsername(dto.getUsername());
        userInfo.setNickName(dto.getUsername());
        userInfo.setCreateTime(LocalDateTime.now());
        userInfo.setUpdateTime(LocalDateTime.now());
        userInfo.setSalt("0");
        userInfoMapper.insert(userInfo);

        // 删除验证码
        verifyCodeManager.removeImgVerifyCode(dto.getSessionId());

        // 生成JWT 并返回
        return RestResp.ok(
            UserRegisterRespDto.builder()
                .token(jwtUtils.generateToken(userInfo.getId(), SystemConfigConsts.NOVEL_FRONT_KEY))
                .uid(userInfo.getId())
                .build()
        );

    }

    @Override
    public RestResp<UserLoginRespDto> login(UserLoginReqDto dto) {
        // 查询用户信息
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.UserInfoTable.COLUMN_USERNAME, dto.getUsername())
            .last(DatabaseConsts.SqlEnum.LIMIT_1.getSql());
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        if (Objects.isNull(userInfo)) {
            // 用户不存在
            throw new BusinessException(ErrorCodeEnum.USER_ACCOUNT_NOT_EXIST);
        }

        // 判断密码是否正确
        if (!Objects.equals(userInfo.getPassword()
            , DigestUtils.md5DigestAsHex(dto.getPassword().getBytes(StandardCharsets.UTF_8)))) {
            // 密码错误
            throw new BusinessException(ErrorCodeEnum.USER_PASSWORD_ERROR);
        }

        // 登录成功，生成JWT并返回
        return RestResp.ok(UserLoginRespDto.builder()
            .token(jwtUtils.generateToken(userInfo.getId(), SystemConfigConsts.NOVEL_FRONT_KEY))
            .uid(userInfo.getId())
            .nickName(userInfo.getNickName()).build());
    }

    @Override
    public RestResp<Void> saveFeedback(Long userId, String content) {
        UserFeedback userFeedback = new UserFeedback();
        userFeedback.setUserId(userId);
        userFeedback.setContent(content);
        userFeedback.setCreateTime(LocalDateTime.now());
        userFeedback.setUpdateTime(LocalDateTime.now());
        userFeedbackMapper.insert(userFeedback);
        return RestResp.ok();
    }

    @Override
    public RestResp<PageRespDto<UserFeedbackRespDto>> listFeedbacks(Long userId, PageReqDto dto) {
        IPage<UserFeedback> page = new Page<>();
        page.setCurrent(dto.getPageNum());
        page.setSize(dto.getPageSize());
        QueryWrapper<UserFeedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.UserFeedBackTable.COLUMN_USER_ID, userId)
                    .orderByDesc(DatabaseConsts.CommonColumnEnum.UPDATE_TIME.getName());
        IPage<UserFeedback> feedbackPage = userFeedbackMapper.selectPage(page, queryWrapper);
        List<UserFeedback> feedbacks = feedbackPage.getRecords();
        return RestResp.ok(PageRespDto.of(dto.getPageNum(), dto.getPageSize(), page.getTotal(),
                feedbacks.stream().map(item -> UserFeedbackRespDto.builder()
                        .id(item.getId())
                        .feedbackContent(item.getContent())
                        .feedbackTime(item.getCreateTime())
                        .build()).toList()));
    }

    @Override
    public RestResp<Void> updateUserInfo(UserInfoUptReqDto dto) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(dto.getUserId());
        userInfo.setNickName(dto.getNickName());
        userInfo.setUserPhoto(dto.getUserPhoto());
        userInfo.setUserSex(dto.getUserSex());
        userInfoMapper.updateById(userInfo);
        return RestResp.ok();
    }

    @Override
    public RestResp<Void> deleteFeedback(Long userId, Long id) {
        QueryWrapper<UserFeedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.CommonColumnEnum.ID.getName(), id)
            .eq(DatabaseConsts.UserFeedBackTable.COLUMN_USER_ID, userId);
        userFeedbackMapper.delete(queryWrapper);
        return RestResp.ok();
    }

    @Override
    public RestResp<Integer> getBookshelfStatus(Long userId, String bookId) {
        QueryWrapper<UserBookshelf> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.UserBookshelfTable.COLUMN_USER_ID, userId)
            .eq(DatabaseConsts.UserBookshelfTable.COLUMN_BOOK_ID, bookId);
        return RestResp.ok(
            userBookshelfMapper.selectCount(queryWrapper) > 0
                ? CommonConsts.YES
                : CommonConsts.NO
        );
    }

    @Override
    public RestResp<UserInfoRespDto> getUserInfo(Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        return RestResp.ok(UserInfoRespDto.builder()
            .nickName(userInfo.getNickName())
            .userSex(userInfo.getUserSex())
            .userPhoto(userInfo.getUserPhoto())
            .accountBalance(userInfo.getAccountBalance())
            .build());
    }

    // 查询书架列表（分页）
    @Override
    public RestResp<PageRespDto<UserBookshelfRespDto>> listBookshelf(Long userId, PageReqDto pageReqDto) {
        // 构建分页对象
        IPage<UserBookshelfRespDto> page = new Page<>(pageReqDto.getPageNum(), pageReqDto.getPageSize());

        // 执行自定义查询
        List<UserBookshelfRespDto> bookshelfList = userBookshelfMapper.selectUserBookshelf(page, userId);

        // 封装分页响应
        return RestResp.ok(PageRespDto.of(
                pageReqDto.getPageNum(),
                pageReqDto.getPageSize(),
                page.getTotal(),
                bookshelfList
        ));
    }

    @Override
    public RestResp<Void> addBookshelf(UserBookshelfReqDto dto) {
        // 校验书籍是否存在书架
        QueryWrapper<UserBookshelf> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.UserBookshelfTable.COLUMN_USER_ID, UserHolder.getUserId())
                    .eq(DatabaseConsts.UserBookshelfTable.COLUMN_BOOK_ID, dto.getBookId());
        if (userBookshelfMapper.selectCount(queryWrapper) > 0) {
            // 书籍已在书架中
            return RestResp.ok();
        }
        // 设置或获取preContentId
        Long preContentId = dto.getPreContentId();
        if (preContentId == null) {
            // 查询阅读历史，看是否有阅读记录
            QueryWrapper<UserReadHistory> historyQueryWrapper = new QueryWrapper<>();
            historyQueryWrapper.lambda()
                    .eq(UserReadHistory::getUserId, UserHolder.getUserId())
                    .eq(UserReadHistory::getBookId, dto.getBookId());

            UserReadHistory userReadHistory = userReadHistoryMapper.selectOne(historyQueryWrapper);
            if (userReadHistory != null) {
                preContentId = userReadHistory.getPreContentId();
            } else {
                // 没有阅读历史，查询第一章ID
                preContentId = bookChapterMapper.selectFirstChapterId(Long.valueOf(dto.getBookId()));
            }

        }
        UserBookshelf userBookshelf = new UserBookshelf();
        userBookshelf.setUserId(UserHolder.getUserId());
        userBookshelf.setBookId(Long.valueOf(dto.getBookId()));
        userBookshelf.setPreContentId(preContentId);
        userBookshelf.setCreateTime(LocalDateTime.now());
        userBookshelf.setUpdateTime(LocalDateTime.now());
        userBookshelfMapper.insert(userBookshelf);
        return RestResp.ok();
    }

    @Override
    public RestResp<Void> deleteBookshelf(Long userId, String bookId) {
        QueryWrapper<UserBookshelf> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.UserBookshelfTable.COLUMN_USER_ID, userId)
                    .eq(DatabaseConsts.UserBookshelfTable.COLUMN_BOOK_ID, bookId);
        userBookshelfMapper.delete(queryWrapper);
        return RestResp.ok();
    }

    @Override
    public RestResp<Void> setPassword(UserPasswordReqDto dto) {
        // 1. 先校验新密码和确认密码是否一致
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ErrorCodeEnum.USER_PASSWORD_CONFIRM_ERROR);
        }

        // 2. 校验密码长度
        if (dto.getNewPassword().length() < 6 || dto.getNewPassword().length() > 20) {
            throw new BusinessException(ErrorCodeEnum.USER_PASSWORD_LENGTH_ERROR);
        }

        // 3. 校验用户是否存在
        UserInfo userInfo = userInfoMapper.selectById(UserHolder.getUserId());
        if (userInfo == null) {
            throw new BusinessException(ErrorCodeEnum.USER_ACCOUNT_NOT_EXIST);
        }

        // 4. 校验旧密码是否正确
        String oldPasswordMd5 = DigestUtils.md5DigestAsHex(
                dto.getOldPassword().getBytes(StandardCharsets.UTF_8)
        );
        if (!oldPasswordMd5.equals(userInfo.getPassword())) {
            throw new BusinessException(ErrorCodeEnum.USER_PASSWORD_ERROR);
        }

        // 5. 加密新密码
        String newPasswordMd5 = DigestUtils.md5DigestAsHex(
                dto.getNewPassword().getBytes(StandardCharsets.UTF_8)
        );

        // 6. 验证新密码不能与原密码相同
        if (userInfo.getPassword().equals(newPasswordMd5)) {
            throw new BusinessException(ErrorCodeEnum.USER_NEW_PASSWORD_SAME);
        }

        // 7. 更新密码
        UserInfo updateUser = new UserInfo();
        updateUser.setId(UserHolder.getUserId());
        updateUser.setPassword(newPasswordMd5);
        updateUser.setUpdateTime(LocalDateTime.now());

        int rows = userInfoMapper.updateById(updateUser);
        if (rows == 0) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        return RestResp.ok();
    }

    @Override
    public RestResp<Void> addReadHistory(UserReadHistoryReqDto dto) {
        // 检查书籍是否存在
        BookInfo bookInfo = bookInfoMapper.selectById(dto.getBookId());
        if (bookInfo == null) {
            return RestResp.fail(ErrorCodeEnum.BOOK_NOT_FOUND);
        }
        // 查询是否已存在该书的阅读历史
        QueryWrapper<UserReadHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserReadHistory::getUserId, UserHolder.getUserId())
                .eq(UserReadHistory::getBookId, dto.getBookId());
        UserReadHistory userReadHistory = userReadHistoryMapper.selectOne(queryWrapper);
        if (userReadHistory != null) {
            // 更新现有记录
            userReadHistory.setPreContentId(dto.getPreContentId());
            userReadHistory.setUpdateTime(LocalDateTime.now());
            userReadHistoryMapper.updateById(userReadHistory);
        } else {
            // 创建新记录
            UserReadHistory newReadHistory = new UserReadHistory();
            newReadHistory.setUserId(UserHolder.getUserId());
            newReadHistory.setBookId(dto.getBookId());
            newReadHistory.setPreContentId(dto.getPreContentId());
            newReadHistory.setCreateTime(LocalDateTime.now());
            newReadHistory.setUpdateTime(LocalDateTime.now());
            userReadHistoryMapper.insert(newReadHistory);
        }
        // 同步更新书架中的阅读进度
        QueryWrapper<UserBookshelf> bookshelfQueryWrapper = new QueryWrapper<>();
        bookshelfQueryWrapper.lambda()
                .eq(UserBookshelf::getUserId, UserHolder.getUserId())
                .eq(UserBookshelf::getBookId, dto.getBookId());

        UserBookshelf userBookshelf = userBookshelfMapper.selectOne(bookshelfQueryWrapper);

        if (userBookshelf != null) {
            // 更新书架中的阅读进度
            userBookshelf.setPreContentId(dto.getPreContentId());
            userBookshelf.setUpdateTime(LocalDateTime.now());
            userBookshelfMapper.updateById(userBookshelf);
        }
        return RestResp.ok();
    }

    @Override
    public RestResp<PageRespDto<UserPayLogRespDto>> listPayLogs(Long userId, PageReqDto dto) {
        IPage<UserPayLog> page = new Page<>();
        page.setCurrent(dto.getPageNum());
        page.setSize(dto.getPageSize());
        QueryWrapper<UserPayLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserPayLog::getUserId, userId)
                .orderByDesc(UserPayLog::getPayTime)
                .orderByDesc(UserPayLog::getUpdateTime);
        IPage<UserPayLog> payLogPage = userPayLogMapper.selectPage(page, queryWrapper);
        List<UserPayLog> payLogs = payLogPage.getRecords();
        return RestResp.ok(PageRespDto.of(dto.getPageNum(), dto.getPageSize(), page.getTotal(),
                payLogs.stream().map(item -> UserPayLogRespDto.builder()
                        .userId(item.getUserId())
                        .outTradeNo(item.getOutTradeNo())
                        .amount(item.getAmount())
                        .productType(item.getProductType())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productValue(item.getProductValue())
                        .payTime(item.getPayTime())
                        .build()).toList()));
    }

    @Override
    public RestResp<PageRespDto<UserConsumeLogRespDto>> listConsumeLogs(Long userId, PageReqDto dto) {
        IPage<UserConsumeLog> page = new Page<>();
        page.setCurrent(dto.getPageNum());
        page.setSize(dto.getPageSize());
        QueryWrapper<UserConsumeLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserConsumeLog::getUserId, userId)
                .orderByDesc(UserConsumeLog::getUpdateTime);
        IPage<UserConsumeLog> consumeLogPage = userConsumeLogMapper.selectPage(page, queryWrapper);
        List<UserConsumeLog> consumeLogs = consumeLogPage.getRecords();
        return RestResp.ok(PageRespDto.of(dto.getPageNum(), dto.getPageSize(), page.getTotal(),
                consumeLogs.stream().map(item -> UserConsumeLogRespDto.builder()
                        .userId(item.getUserId())
                        .amount(item.getAmount())
                        .productType(item.getProductType())
                        .productId(item.getProductId())
                        .producName(item.getProducName())
                        .producValue(item.getProducValue())
                        .build()).toList()));
    }
}
