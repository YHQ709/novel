package io.github.xxyopen.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.xxyopen.novel.core.annotation.Key;
import io.github.xxyopen.novel.core.annotation.Lock;
import io.github.xxyopen.novel.core.auth.UserHolder;
import io.github.xxyopen.novel.core.common.constant.ErrorCodeEnum;
import io.github.xxyopen.novel.core.common.exception.BusinessException;
import io.github.xxyopen.novel.core.common.req.PageReqDto;
import io.github.xxyopen.novel.core.common.resp.PageRespDto;
import io.github.xxyopen.novel.core.common.resp.RestResp;
import io.github.xxyopen.novel.core.constant.CacheConsts;
import io.github.xxyopen.novel.core.constant.DatabaseConsts;
import io.github.xxyopen.novel.dao.entity.*;
import io.github.xxyopen.novel.dao.mapper.*;
import io.github.xxyopen.novel.dto.AuthorInfoDto;
import io.github.xxyopen.novel.dto.req.*;
import io.github.xxyopen.novel.dto.resp.*;
import io.github.xxyopen.novel.manager.cache.*;
import io.github.xxyopen.novel.manager.dao.UserDaoManager;
import io.github.xxyopen.novel.manager.mq.AmqpMsgManager;
import io.github.xxyopen.novel.service.AuthorService;
import io.github.xxyopen.novel.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 小说模块 服务实现类
 *
 * @author xiongxiaoyang
 * @date 2022/5/14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookServiceImpl implements BookService {

    private final BookCategoryCacheManager bookCategoryCacheManager;

    private final BookRankCacheManager bookRankCacheManager;

    private final BookInfoCacheManager bookInfoCacheManager;

    private final BookChapterCacheManager bookChapterCacheManager;

    private final BookContentCacheManager bookContentCacheManager;

    private final AuthorInfoCacheManager authorInfoCacheManager;

    private final BookInfoMapper bookInfoMapper;

    private final BookChapterMapper bookChapterMapper;

    private final BookContentMapper bookContentMapper;

    private final BookCommentMapper bookCommentMapper;

    private final UserDaoManager userDaoManager;

    private final AmqpMsgManager amqpMsgManager;

    private static final Integer REC_BOOK_COUNT = 4;

    private static final Integer VIP_CHAPTER_PRICE = 10;

    private final BookCommentReplyMapper bookCommentReplyMapper;

    private final UserConsumeLogMapper userConsumeLogMapper;

    private final UserInfoMapper userInfoMapper;

    private final AuthorService authorService;

    @Override
    public RestResp<List<BookRankRespDto>> listVisitRankBooks() {
        return RestResp.ok(bookRankCacheManager.listVisitRankBooks());
    }

    @Override
    public RestResp<List<BookRankRespDto>> listNewestRankBooks() {
        return RestResp.ok(bookRankCacheManager.listNewestRankBooks());
    }

    @Override
    public RestResp<List<BookRankRespDto>> listUpdateRankBooks() {
        return RestResp.ok(bookRankCacheManager.listUpdateRankBooks());
    }

    @Override
    public RestResp<BookInfoRespDto> getBookById(Long bookId) {
        return RestResp.ok(bookInfoCacheManager.getBookInfo(bookId));
    }

    @Override
    public RestResp<BookChapterAboutRespDto> getLastChapterAbout(Long bookId) {
        // 查询小说信息
        BookInfoRespDto bookInfo = bookInfoCacheManager.getBookInfo(bookId);

        // 查询最新章节信息
        BookChapterRespDto bookChapter = bookChapterCacheManager.getChapter(
            bookInfo.getLastChapterId());

        // 查询章节内容
        String content = bookContentCacheManager.getBookContent(bookInfo.getLastChapterId());

        // 查询章节总数
        QueryWrapper<BookChapter> chapterQueryWrapper = new QueryWrapper<>();
        chapterQueryWrapper.eq(DatabaseConsts.BookChapterTable.COLUMN_BOOK_ID, bookId);
        Long chapterTotal = bookChapterMapper.selectCount(chapterQueryWrapper);

        // 组装数据并返回
        return RestResp.ok(BookChapterAboutRespDto.builder()
            .chapterInfo(bookChapter)
            .chapterTotal(chapterTotal)
            .contentSummary(content.substring(0, 30))
            .build());
    }

    @Override
    public RestResp<List<BookInfoRespDto>> listRecBooks(Long bookId)
        throws NoSuchAlgorithmException {
        Long categoryId = bookInfoCacheManager.getBookInfo(bookId).getCategoryId();
        List<Long> lastUpdateIdList = bookInfoCacheManager.getLastUpdateIdList(categoryId);

        // 检查列表是否为空或不足
        if (CollectionUtils.isEmpty(lastUpdateIdList)) {
            return RestResp.ok(Collections.emptyList());
        }

        // 排除当前书籍，同时确保有足够的推荐书籍用于展示
        List<Long> candidateIdList = lastUpdateIdList.stream()
                .filter(id -> !Objects.equals(id, bookId))
                .toList();

        if (candidateIdList.isEmpty()) {
            return RestResp.ok(Collections.emptyList());
        }

        // 确定实际推荐的书籍数量
        int actualRecCount = Math.min(REC_BOOK_COUNT, candidateIdList.size());

        List<BookInfoRespDto> respDtoList = new ArrayList<>();
        Set<Integer> recIdIndexSet = new HashSet<>();
        Random rand = SecureRandom.getInstanceStrong();

        // 使用 Set 提高查找效率，同时修复bug防止无限循环
        while (respDtoList.size() < actualRecCount && recIdIndexSet.size() < candidateIdList.size()) {
            int recIdIndex = rand.nextInt(candidateIdList.size());
            if (!recIdIndexSet.contains(recIdIndex)) {
                recIdIndexSet.add(recIdIndex);
                Long recBookId = candidateIdList.get(recIdIndex);
                BookInfoRespDto bookInfo = bookInfoCacheManager.getBookInfo(recBookId);
                respDtoList.add(bookInfo);
            }
        }

        return RestResp.ok(respDtoList);
    }

    @Override
    public RestResp<Void> addVisitCount(Long bookId) {
        bookInfoMapper.addVisitCount(bookId);
        return RestResp.ok();
    }

    @Override
    public RestResp<Long> getPreChapterId(Long chapterId) {
        // 查询小说ID 和 章节号
        BookChapterRespDto chapter = bookChapterCacheManager.getChapter(chapterId);
        Long bookId = chapter.getBookId();
        Integer chapterNum = chapter.getChapterNum();

        // 查询上一章信息并返回章节ID
        QueryWrapper<BookChapter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.BookChapterTable.COLUMN_BOOK_ID, bookId)
            .lt(DatabaseConsts.BookChapterTable.COLUMN_CHAPTER_NUM, chapterNum)
            .orderByDesc(DatabaseConsts.BookChapterTable.COLUMN_CHAPTER_NUM)
            .last(DatabaseConsts.SqlEnum.LIMIT_1.getSql());
        return RestResp.ok(
            Optional.ofNullable(bookChapterMapper.selectOne(queryWrapper))
                .map(BookChapter::getId)
                .orElse(null)
        );
    }

    @Override
    public RestResp<Long> getNextChapterId(Long chapterId) {
        // 查询小说ID 和 章节号
        BookChapterRespDto chapter = bookChapterCacheManager.getChapter(chapterId);
        Long bookId = chapter.getBookId();
        Integer chapterNum = chapter.getChapterNum();

        // 查询下一章信息并返回章节ID
        QueryWrapper<BookChapter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.BookChapterTable.COLUMN_BOOK_ID, bookId)
            .gt(DatabaseConsts.BookChapterTable.COLUMN_CHAPTER_NUM, chapterNum)
            .orderByAsc(DatabaseConsts.BookChapterTable.COLUMN_CHAPTER_NUM)
            .last(DatabaseConsts.SqlEnum.LIMIT_1.getSql());
        return RestResp.ok(
            Optional.ofNullable(bookChapterMapper.selectOne(queryWrapper))
                .map(BookChapter::getId)
                .orElse(null)
        );
    }

    @Override
    public RestResp<List<BookChapterRespDto>> listChapters(Long bookId) {
        QueryWrapper<BookChapter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.BookChapterTable.COLUMN_BOOK_ID, bookId)
            .orderByAsc(DatabaseConsts.BookChapterTable.COLUMN_CHAPTER_NUM);
        return RestResp.ok(bookChapterMapper.selectList(queryWrapper).stream()
            .map(v -> BookChapterRespDto.builder()
                .id(v.getId())
                .chapterName(v.getChapterName())
                .isVip(v.getIsVip())
                .build()).toList());
    }

    @Override
    public RestResp<List<BookCategoryRespDto>> listCategory(Integer workDirection) {
        return RestResp.ok(bookCategoryCacheManager.listCategory(workDirection));
    }

    @Lock(prefix = "userComment")
    @Override
    public RestResp<Void> saveComment(
        @Key(expr = "#{userId + '::' + bookId}") UserCommentReqDto dto) {
        // 校验书籍是否存在
        BookInfo bookInfo = bookInfoMapper.selectById(dto.getBookId());
        if (bookInfo == null) {
            return RestResp.fail(ErrorCodeEnum.BOOK_NOT_FOUND);
        }
        // 校验用户是否已发表评论
        QueryWrapper<BookComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.BookCommentTable.COLUMN_USER_ID, dto.getUserId())
            .eq(DatabaseConsts.BookCommentTable.COLUMN_BOOK_ID, dto.getBookId());
        if (bookCommentMapper.selectCount(queryWrapper) > 0) {
            // 用户已发表评论
            return RestResp.fail(ErrorCodeEnum.USER_COMMENTED);
        }
        BookComment bookComment = new BookComment();
        bookComment.setBookId(dto.getBookId());
        bookComment.setUserId(dto.getUserId());
        bookComment.setCommentContent(dto.getCommentContent());
        bookComment.setCreateTime(LocalDateTime.now());
        bookComment.setUpdateTime(LocalDateTime.now());
        bookCommentMapper.insert(bookComment);
        return RestResp.ok();
    }

    @Override
    public RestResp<PageRespDto<BookCommentRespDto.CommentInfo>> listNewestComments(Long bookId, PageReqDto dto) {

        // 构建分页对象
        IPage<BookComment> page = new Page<>();
        page.setCurrent(dto.getPageNum());
        page.setSize(dto.getPageSize());

        // 构建查询条件（按时间倒序，最新的在前）
        QueryWrapper<BookComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.BookCommentTable.COLUMN_BOOK_ID, bookId)
                .orderByDesc(DatabaseConsts.CommonColumnEnum.CREATE_TIME.getName());

        // 执行分页查询
        IPage<BookComment> commentPage = bookCommentMapper.selectPage(page, queryWrapper);
        List<BookComment> commentList = commentPage.getRecords();

        List<BookCommentRespDto.CommentInfo> commentInfos = Collections.emptyList();
        if (!CollectionUtils.isEmpty(commentList)) {
            // 获取所有评论用户ID
            List<Long> userIds = commentList.stream()
                    .map(BookComment::getUserId)
                    .toList();

            // 查询用户信息
            List<UserInfo> userInfos = userDaoManager.listUsers(userIds);
            Map<Long, UserInfo> userInfoMap = userInfos.stream()
                    .collect(Collectors.toMap(UserInfo::getId, Function.identity()));

            // 转换为评论列表DTO
            commentInfos = commentList.stream()
                    .map(comment -> {
                        UserInfo userInfo = userInfoMap.get(comment.getUserId());
                        return BookCommentRespDto.CommentInfo.builder()
                                .id(comment.getId())
                                .replyCount(comment.getReplyCount())
                                .commentContent(comment.getCommentContent())
                                .commentUserId(comment.getUserId())
                                .commentUser(userInfo != null ? userInfo.getUsername() : null)
                                .commentUserPhoto(userInfo != null ? userInfo.getUserPhoto() : null)
                                .commentTime(comment.getCreateTime())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        // 构建分页响应
        PageRespDto<BookCommentRespDto.CommentInfo> pageRespDto = PageRespDto.of(
                dto.getPageNum(),
                dto.getPageSize(),
                commentPage.getTotal(),
                commentInfos
        );

        return RestResp.ok(pageRespDto);
    }

    @Override
    public RestResp<Void> deleteComment(Long userId, Long commentId) {
        QueryWrapper<BookComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.CommonColumnEnum.ID.getName(), commentId)
            .eq(DatabaseConsts.BookCommentTable.COLUMN_USER_ID, userId);
        bookCommentMapper.delete(queryWrapper);
        return RestResp.ok();
    }

    @Override
    public RestResp<Void> updateComment(Long userId, Long id, String content) {
        QueryWrapper<BookComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.CommonColumnEnum.ID.getName(), id)
            .eq(DatabaseConsts.BookCommentTable.COLUMN_USER_ID, userId);
        BookComment bookComment = new BookComment();
        bookComment.setCommentContent(content);
        bookCommentMapper.update(bookComment, queryWrapper);
        return RestResp.ok();
    }

    @Override
    public RestResp<Void> saveBook(BookAddReqDto dto) {
        // 校验小说名是否已存在
        QueryWrapper<BookInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.BookTable.COLUMN_BOOK_NAME, dto.getBookName());
        if (bookInfoMapper.selectCount(queryWrapper) > 0) {
            return RestResp.fail(ErrorCodeEnum.AUTHOR_BOOK_NAME_EXIST);
        }
        BookInfo bookInfo = new BookInfo();
        // 设置作家信息
        AuthorInfoDto author = authorInfoCacheManager.getAuthor(UserHolder.getUserId());
        bookInfo.setAuthorId(author.getId());
        bookInfo.setAuthorName(author.getPenName());
        // 设置其他信息
        bookInfo.setWorkDirection(dto.getWorkDirection());
        bookInfo.setCategoryId(dto.getCategoryId());
        bookInfo.setCategoryName(dto.getCategoryName());
        bookInfo.setBookName(dto.getBookName());
        bookInfo.setPicUrl(dto.getPicUrl());
        bookInfo.setBookDesc(dto.getBookDesc());
        bookInfo.setIsVip(dto.getIsVip());
        bookInfo.setBookStatus(0);
        bookInfo.setScore(0);
        bookInfo.setVisitCount(0L);
        bookInfo.setCreateTime(LocalDateTime.now());
        bookInfo.setUpdateTime(LocalDateTime.now());
        // 保存小说信息
        bookInfoMapper.insert(bookInfo);
        return RestResp.ok();
    }

    @Transactional
    @Override
    public RestResp<Void> updateBook(Long bookId, BookUpdateReqDto dto) {
        // 1. 查询小说是否存在
        BookInfo bookInfo = bookInfoMapper.selectById(bookId);
        if (bookInfo == null) {
            return RestResp.fail(ErrorCodeEnum.BOOK_NOT_FOUND);
        }

        // 2. 获取当前作家信息
        AuthorInfoDto author = authorInfoCacheManager.getAuthor(UserHolder.getUserId());

        // 3. 校验权限：只能修改自己的小说
        if (!bookInfo.getAuthorId().equals(author.getId())) {
            // 访问未授权
            return RestResp.fail(ErrorCodeEnum.USER_UN_AUTH);
        }

        // 4. 校验小说名是否重复
        if (!bookInfo.getBookName().equals(dto.getBookName())) {
            QueryWrapper<BookInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("book_name", dto.getBookName())
                    .eq("author_id", author.getId())
                    .ne("id", bookId);

            if (bookInfoMapper.selectCount(queryWrapper) > 0) {
                return RestResp.fail(ErrorCodeEnum.AUTHOR_BOOK_NAME_EXIST);
            }
        }

        // 5. 更新字段
        bookInfo.setCategoryId(dto.getCategoryId());
        bookInfo.setCategoryName(dto.getCategoryName());
        bookInfo.setBookName(dto.getBookName());
        bookInfo.setPicUrl(dto.getPicUrl());
        bookInfo.setBookDesc(dto.getBookDesc());
        bookInfo.setBookStatus(dto.getBookStatus());
        bookInfo.setUpdateTime(LocalDateTime.now());

        // 6. 执行更新
        bookInfoMapper.updateById(bookInfo);

        return RestResp.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public RestResp<Void> deleteBook(Long bookId) {
        // 1. 查询小说信息
        BookInfoRespDto bookInfo = bookInfoCacheManager.getBookInfo(bookId);
        if (bookInfo == null) {
            return RestResp.fail(ErrorCodeEnum.BOOK_NOT_FOUND);
        }

        // 2. 获取当前作家信息并校验权限
        AuthorInfoDto author = authorInfoCacheManager.getAuthor(UserHolder.getUserId());
        if (!bookInfo.getAuthorId().equals(author.getId())) {
            return RestResp.fail(ErrorCodeEnum.USER_UN_AUTH);
        }

        // 3. 查询该小说的所有章节
        QueryWrapper<BookChapter> chapterQueryWrapper = new QueryWrapper<>();
        chapterQueryWrapper.eq(DatabaseConsts.BookChapterTable.COLUMN_BOOK_ID, bookId);
        List<BookChapter> chapters = bookChapterMapper.selectList(chapterQueryWrapper);

        // 4. 获取所有章节ID
        List<Long> chapterIds = chapters.stream()
                .map(BookChapter::getId)
                .collect(Collectors.toList());

        // 5. 删除章节内容（如果有关联的章节内容表）
        if (!chapterIds.isEmpty()) {
            QueryWrapper<BookContent> contentQueryWrapper = new QueryWrapper<>();
            contentQueryWrapper.in(DatabaseConsts.BookContentTable.COLUMN_CHAPTER_ID, chapterIds);
            bookContentMapper.delete(contentQueryWrapper);
        }

        // 6. 删除章节信息
        if (!chapterIds.isEmpty()) {
            bookChapterMapper.delete(chapterQueryWrapper);
        }

        // 7. 删除小说信息
        bookInfoMapper.deleteById(bookId);

        // 8. 清理缓存
        // 清理小说信息缓存
        bookInfoCacheManager.evictBookInfoCache(bookId);

        // 清理所有章节缓存
        for (Long chapterId : chapterIds) {
            bookChapterCacheManager.evictBookChapterCache(chapterId);
            bookContentCacheManager.evictBookContentCache(chapterId);
        }
        return RestResp.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public RestResp<Void> saveBookChapter(ChapterAddReqDto dto) {
        // 校验该作品是否属于当前作家
        BookInfo bookInfo = bookInfoMapper.selectById(dto.getBookId());
        if (!Objects.equals(bookInfo.getAuthorId(), UserHolder.getAuthorId())) {
            return RestResp.fail(ErrorCodeEnum.USER_UN_AUTH);
        }
        // 1) 保存章节相关信息到小说章节表
        //  a) 查询最新章节号
        int chapterNum = 0;
        QueryWrapper<BookChapter> chapterQueryWrapper = new QueryWrapper<>();
        chapterQueryWrapper.eq(DatabaseConsts.BookChapterTable.COLUMN_BOOK_ID, dto.getBookId())
            .orderByDesc(DatabaseConsts.BookChapterTable.COLUMN_CHAPTER_NUM)
            .last(DatabaseConsts.SqlEnum.LIMIT_1.getSql());
        BookChapter bookChapter = bookChapterMapper.selectOne(chapterQueryWrapper);
        if (Objects.nonNull(bookChapter)) {
            chapterNum = bookChapter.getChapterNum() + 1;
        }
        //  b) 设置章节相关信息并保存
        BookChapter newBookChapter = new BookChapter();
        newBookChapter.setBookId(dto.getBookId());
        newBookChapter.setChapterName(dto.getChapterName());
        newBookChapter.setChapterNum(chapterNum);
        newBookChapter.setWordCount(dto.getChapterContent().length());
        newBookChapter.setIsVip(dto.getIsVip());
        newBookChapter.setCreateTime(LocalDateTime.now());
        newBookChapter.setUpdateTime(LocalDateTime.now());
        bookChapterMapper.insert(newBookChapter);

        // 2) 保存章节内容到小说内容表
        BookContent bookContent = new BookContent();
//        bookContent.setContent(dto.getChapterContent());
        String content = dto.getChapterContent();
        if (content != null) {
            // 还原被转义的HTML标签
            content = content
                    .replace("&lt;br/&gt;", "<br/>")
                    .replace("&lt;br&gt;", "<br/>")
                    .replace("&amp;nbsp;", "&nbsp;");
        }
        bookContent.setContent(content);
        bookContent.setChapterId(newBookChapter.getId());
        bookContent.setCreateTime(LocalDateTime.now());
        bookContent.setUpdateTime(LocalDateTime.now());
        bookContentMapper.insert(bookContent);

        // 3) 更新小说表最新章节信息和小说总字数信息
        //  a) 更新小说表关于最新章节的信息
        BookInfo newBookInfo = new BookInfo();
        newBookInfo.setId(dto.getBookId());
        newBookInfo.setLastChapterId(newBookChapter.getId());
        newBookInfo.setLastChapterName(newBookChapter.getChapterName());
        newBookInfo.setLastChapterUpdateTime(LocalDateTime.now());
        newBookInfo.setWordCount(bookInfo.getWordCount() + newBookChapter.getWordCount());
        newBookChapter.setUpdateTime(LocalDateTime.now());
        bookInfoMapper.updateById(newBookInfo);
        //  b) 清除小说信息缓存
        bookInfoCacheManager.evictBookInfoCache(dto.getBookId());
        //  c) 发送小说信息更新的 MQ 消息
        amqpMsgManager.sendBookChangeMsg(dto.getBookId());
        return RestResp.ok();
    }

    @Override
    public RestResp<PageRespDto<BookInfoRespDto>> listAuthorBooks(PageReqDto dto) {
        IPage<BookInfo> page = new Page<>();
        page.setCurrent(dto.getPageNum());
        page.setSize(dto.getPageSize());
        QueryWrapper<BookInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.BookTable.AUTHOR_ID, UserHolder.getAuthorId())
            .orderByDesc(DatabaseConsts.CommonColumnEnum.CREATE_TIME.getName());
        IPage<BookInfo> bookInfoPage = bookInfoMapper.selectPage(page, queryWrapper);
        return RestResp.ok(PageRespDto.of(dto.getPageNum(), dto.getPageSize(), page.getTotal(),
            bookInfoPage.getRecords().stream().map(v -> BookInfoRespDto.builder()
                .id(v.getId())
                .bookName(v.getBookName())
                .picUrl(v.getPicUrl())
                .categoryName(v.getCategoryName())
                .wordCount(v.getWordCount())
                .visitCount(v.getVisitCount())
                .bookStatus(v.getBookStatus())
                .updateTime(v.getUpdateTime())
                .build()).toList()));
    }

    @Override
    public RestResp<PageRespDto<BookChapterRespDto>> listBookChapters(Long bookId, PageReqDto dto) {
        IPage<BookChapter> page = new Page<>();
        page.setCurrent(dto.getPageNum());
        page.setSize(dto.getPageSize());
        QueryWrapper<BookChapter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.BookChapterTable.COLUMN_BOOK_ID, bookId)
            .orderByDesc(DatabaseConsts.BookChapterTable.COLUMN_CHAPTER_NUM);
        IPage<BookChapter> bookChapterPage = bookChapterMapper.selectPage(page, queryWrapper);
        return RestResp.ok(PageRespDto.of(dto.getPageNum(), dto.getPageSize(), page.getTotal(),
            bookChapterPage.getRecords().stream().map(v -> BookChapterRespDto.builder()
                .id(v.getId())
                .chapterName(v.getChapterName())
                .chapterUpdateTime(v.getUpdateTime())
                .isVip(v.getIsVip())
                .build()).toList()));
    }

    @Override
    public RestResp<PageRespDto<UserCommentRespDto>> listComments(Long userId, PageReqDto pageReqDto) {
        IPage<BookComment> page = new Page<>();
        page.setCurrent(pageReqDto.getPageNum());
        page.setSize(pageReqDto.getPageSize());
        QueryWrapper<BookComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DatabaseConsts.BookCommentTable.COLUMN_USER_ID, userId)
            .orderByDesc(DatabaseConsts.CommonColumnEnum.UPDATE_TIME.getName());
        IPage<BookComment> bookCommentPage = bookCommentMapper.selectPage(page, queryWrapper);
        List<BookComment> comments = bookCommentPage.getRecords();
        
        List<UserCommentRespDto> commentRespDtoList = Collections.emptyList();
        if (!CollectionUtils.isEmpty(comments)) {
            List<Long> bookIds = comments.stream().map(BookComment::getBookId).toList();
            QueryWrapper<BookInfo> bookInfoQueryWrapper = new QueryWrapper<>();
            bookInfoQueryWrapper.in(DatabaseConsts.CommonColumnEnum.ID.getName(), bookIds);
            Map<Long, BookInfo> bookInfoMap = bookInfoMapper.selectList(bookInfoQueryWrapper).stream()
                .collect(Collectors.toMap(BookInfo::getId, Function.identity()));
            
            commentRespDtoList = comments.stream().map(v -> {
                BookInfo bookInfo = bookInfoMap.get(v.getBookId());
                return UserCommentRespDto.builder()
                    .id(v.getId())
                    .commentContent(v.getCommentContent())
                    .commentBookId(String.valueOf(bookInfo != null ? bookInfo.getId() : null))
                    .commentBook(bookInfo != null ? bookInfo.getBookName() : null)
                    .commentBookPic(bookInfo != null ? bookInfo.getPicUrl() : null)
                    .commentTime(v.getCreateTime())
                    .build();
            }).toList();
        }
        
        return RestResp.ok(PageRespDto.of(pageReqDto.getPageNum(), pageReqDto.getPageSize(), 
            page.getTotal(), commentRespDtoList));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public RestResp<Void> deleteBookChapter(Long chapterId) {
        // 1.查询章节信息
        BookChapterRespDto chapter = bookChapterCacheManager.getChapter(chapterId);
        // 2.查询小说信息
        BookInfoRespDto bookInfo = bookInfoCacheManager.getBookInfo(chapter.getBookId());
        // 3.删除章节信息
        bookChapterMapper.deleteById(chapterId);
        // 4.删除章节内容
        QueryWrapper<BookContent> bookContentQueryWrapper = new QueryWrapper<>();
        bookContentQueryWrapper.eq(DatabaseConsts.BookContentTable.COLUMN_CHAPTER_ID, chapterId);
        bookContentMapper.delete(bookContentQueryWrapper);
        // 5.更新小说信息
        BookInfo newBookInfo = new BookInfo();
        newBookInfo.setId(chapter.getBookId());
        newBookInfo.setUpdateTime(LocalDateTime.now());
        newBookInfo.setWordCount(bookInfo.getWordCount() - chapter.getChapterWordCount());
        if (Objects.equals(bookInfo.getLastChapterId(), chapterId)) {
            // 设置最新章节信息
            QueryWrapper<BookChapter> bookChapterQueryWrapper = new QueryWrapper<>();
            bookChapterQueryWrapper.eq(DatabaseConsts.BookChapterTable.COLUMN_BOOK_ID, chapter.getBookId())
                .orderByDesc(DatabaseConsts.BookChapterTable.COLUMN_CHAPTER_NUM)
                .last(DatabaseConsts.SqlEnum.LIMIT_1.getSql());
            BookChapter bookChapter = bookChapterMapper.selectOne(bookChapterQueryWrapper);
            Long lastChapterId = 0L;
            String lastChapterName = "";
            LocalDateTime lastChapterUpdateTime = null;
            if (Objects.nonNull(bookChapter)) {
                lastChapterId = bookChapter.getId();
                lastChapterName = bookChapter.getChapterName();
                lastChapterUpdateTime = bookChapter.getUpdateTime();
            }
            newBookInfo.setLastChapterId(lastChapterId);
            newBookInfo.setLastChapterName(lastChapterName);
            newBookInfo.setLastChapterUpdateTime(lastChapterUpdateTime);
        }
        bookInfoMapper.updateById(newBookInfo);
        // 6.清理章节信息缓存
        bookChapterCacheManager.evictBookChapterCache(chapterId);
        // 7.清理章节内容缓存
        bookContentCacheManager.evictBookContentCache(chapterId);
        // 8.清理小说信息缓存
        bookInfoCacheManager.evictBookInfoCache(chapter.getBookId());
        // 9.发送小说信息更新的 MQ 消息
        amqpMsgManager.sendBookChangeMsg(chapter.getBookId());
        return RestResp.ok();
    }

    @Override
    public RestResp<ChapterContentRespDto> getBookChapter(Long chapterId) {
        BookChapterRespDto chapter = bookChapterCacheManager.getChapter(chapterId);
        String bookContent = bookContentCacheManager.getBookContent(chapterId);
        return RestResp.ok(
            ChapterContentRespDto.builder()
                .chapterName(chapter.getChapterName())
                .chapterContent(bookContent)
                .isVip(chapter.getIsVip())
                .build());
    }

    @Transactional
    @Override
    public RestResp<Void> updateBookChapter(Long chapterId, ChapterUpdateReqDto dto) {
        // 1.查询章节信息
        BookChapterRespDto chapter = bookChapterCacheManager.getChapter(chapterId);
        // 2.查询小说信息
        BookInfoRespDto bookInfo = bookInfoCacheManager.getBookInfo(chapter.getBookId());
        // 3.更新章节信息
        BookChapter newChapter = new BookChapter();
        newChapter.setId(chapterId);
        newChapter.setChapterName(dto.getChapterName());
        newChapter.setWordCount(dto.getChapterContent().length());
        newChapter.setIsVip(dto.getIsVip());
        newChapter.setUpdateTime(LocalDateTime.now());
        bookChapterMapper.updateById(newChapter);
        // 4.更新章节内容
        BookContent newContent = new BookContent();
//        newContent.setContent(dto.getChapterContent());
        String content = dto.getChapterContent();
        if (content != null) {
            // 还原被转义的HTML标签
            content = content
                    .replace("&lt;br/&gt;", "<br/>")
                    .replace("&lt;br&gt;", "<br/>")
                    .replace("&amp;nbsp;", "&nbsp;");
        }

        newContent.setContent(content);
        newContent.setUpdateTime(LocalDateTime.now());
        QueryWrapper<BookContent> bookContentQueryWrapper = new QueryWrapper<>();
        bookContentQueryWrapper.eq(DatabaseConsts.BookContentTable.COLUMN_CHAPTER_ID, chapterId);
        bookContentMapper.update(newContent, bookContentQueryWrapper);
        // 5.更新小说信息
        BookInfo newBookInfo = new BookInfo();
        newBookInfo.setId(chapter.getBookId());
        newBookInfo.setUpdateTime(LocalDateTime.now());
        newBookInfo.setWordCount(
            bookInfo.getWordCount() - chapter.getChapterWordCount() + dto.getChapterContent().length());
        if (Objects.equals(bookInfo.getLastChapterId(), chapterId)) {
            // 更新最新章节信息
            newBookInfo.setLastChapterName(dto.getChapterName());
            newBookInfo.setLastChapterUpdateTime(LocalDateTime.now());
        }
        bookInfoMapper.updateById(newBookInfo);
        // 6.清理章节信息缓存
        bookChapterCacheManager.evictBookChapterCache(chapterId);
        // 7.清理章节内容缓存
        bookContentCacheManager.evictBookContentCache(chapterId);
        // 8.清理小说信息缓存
        bookInfoCacheManager.evictBookInfoCache(chapter.getBookId());
        // 9.发送小说信息更新的 MQ 消息
        amqpMsgManager.sendBookChangeMsg(chapter.getBookId());
        return RestResp.ok();
    }

    /**
     * 检查用户是否购买了指定章节
     * 使用Redis缓存，缓存7天
     */
    @Cacheable(value = CacheConsts.USER_PURCHASE_CACHE_NAME,
            key = "#userId + ':' + #chapterId",
            unless = "#result == false",
            cacheManager = CacheConsts.REDIS_CACHE_MANAGER)
    public boolean checkUserPurchased(Long userId, Long chapterId) {
        if (userId == null) {
            return false;
        }

        log.debug("检查用户购买记录: userId={}, chapterId={}", userId, chapterId);

        QueryWrapper<UserConsumeLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserConsumeLog::getUserId, userId)
                .eq(UserConsumeLog::getProductId, chapterId)
                .eq(UserConsumeLog::getProductType, 0);

        return userConsumeLogMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public RestResp<BookContentAboutRespDto> getBookContentAbout(Long chapterId) {
        log.debug("userId:{}", UserHolder.getUserId());
        // 查询章节信息
        BookChapterRespDto bookChapter = bookChapterCacheManager.getChapter(chapterId);

        String content;
        // 判断是否是VIP章节
        if (bookChapter.getIsVip() == 1) {
            // VIP章节，判断用户是否有权限阅读
            if (UserHolder.getUserId() != null && checkUserPurchased(UserHolder.getUserId(), chapterId)) {
                // 用户已登录且已购买，返回章节内容
                content = bookContentCacheManager.getBookContent(chapterId);
                log.debug("用户已购买VIP章节，返回内容");
            } else {
                // 未登录或未购买，不返回内容
                content = "";
                log.debug("用户未购买VIP章节，内容不返回");
            }
        }else {
            // 免费章节，直接返回内容（已缓存）
            content = bookContentCacheManager.getBookContent(chapterId);
            log.debug("免费章节，直接返回内容");
        }

        // 查询小说信息
        BookInfoRespDto bookInfo = bookInfoCacheManager.getBookInfo(bookChapter.getBookId());

        // 组装数据并返回
        return RestResp.ok(BookContentAboutRespDto.builder()
            .bookInfo(bookInfo)
            .chapterInfo(bookChapter)
            .bookContent(content)
            .build());
    }

    @Lock(prefix = "userCommentReply")
    @Override
    public RestResp<Void> saveCommentReply(
            @Key(expr = "#{userId + '::' + commentId}") UserCommentReplyReqDto dto) {
        // 校验评论否存在
        BookComment bookComment = bookCommentMapper.selectById(dto.getCommentId());
        if (bookComment == null) {
            return RestResp.fail(ErrorCodeEnum.USER_COMMENT_NOT_FOUND);
        }
        BookCommentReply bookCommentReply = new BookCommentReply();
        bookCommentReply.setCommentId(dto.getCommentId());
        bookCommentReply.setUserId(dto.getUserId());
        bookCommentReply.setReplyContent(dto.getReplyContent());
        bookCommentReply.setCreateTime(LocalDateTime.now());
        bookCommentReply.setUpdateTime(LocalDateTime.now());
        bookCommentReplyMapper.insert(bookCommentReply);

        // 更新评论的回复数量（+1）
        UpdateWrapper<BookComment> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", dto.getCommentId())
                .setSql("reply_count = reply_count + 1")
                .set("update_time", LocalDateTime.now());
        bookCommentMapper.update(null, updateWrapper);
        return RestResp.ok();
    }

    @Override
    public RestResp<PageRespDto<BookCommentReplyRespDto.CommentReplyInfo>> listNewestCommentReply(Long commentId, PageReqDto dto) {

        // 构建分页对象
        IPage<BookCommentReply> page = new Page<>();
        page.setCurrent(dto.getPageNum());
        page.setSize(dto.getPageSize());

        // 构建查询条件
        QueryWrapper<BookCommentReply> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("comment_id", commentId)
                .orderByDesc(DatabaseConsts.CommonColumnEnum.CREATE_TIME.getName());

        // 执行分页查询
        IPage<BookCommentReply> replyPage = bookCommentReplyMapper.selectPage(page, queryWrapper);
        List<BookCommentReply> replyList = replyPage.getRecords();

        List<BookCommentReplyRespDto.CommentReplyInfo> replyInfos = Collections.emptyList();
        if (!CollectionUtils.isEmpty(replyList)) {
            // 获取所有回复用户ID
            List<Long> userIds = replyList.stream()
                    .map(BookCommentReply::getUserId)
                    .toList();

            // 查询用户信息
            List<UserInfo> userInfos = userDaoManager.listUsers(userIds);
            Map<Long, UserInfo> userInfoMap = userInfos.stream()
                    .collect(Collectors.toMap(UserInfo::getId, Function.identity()));

            // 转换为回复列表DTO
            replyInfos = replyList.stream()
                    .map(reply -> {
                        UserInfo userInfo = userInfoMap.get(reply.getUserId());
                        return BookCommentReplyRespDto.CommentReplyInfo.builder()
                                .id(reply.getId())
                                .replyContent(reply.getReplyContent())
                                .replyUserId(reply.getUserId())
                                .replyUser(userInfo != null ? userInfo.getUsername() : null)
                                .replyUserPhoto(userInfo != null ? userInfo.getUserPhoto() : null)
                                .replyTime(reply.getCreateTime())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        // 构建分页响应
        PageRespDto<BookCommentReplyRespDto.CommentReplyInfo> pageRespDto = PageRespDto.of(
                dto.getPageNum(),
                dto.getPageSize(),
                replyPage.getTotal(),
                replyInfos
        );

        return RestResp.ok(pageRespDto);
    }

    @Override
    public RestResp<BookCommentDetailRespDto> getCommentDetail(Long commentId) {
        // 查询评论
        BookComment bookComment = bookCommentMapper.selectById(commentId);

        // 使用 listUsers 方法查询用户信息（即使只查一个用户）
        List<UserInfo> userInfos = userDaoManager.listUsers(List.of(bookComment.getUserId()));
        UserInfo userInfo = userInfos != null && !userInfos.isEmpty() ? userInfos.get(0) : null;

        // 构建返回对象
        BookCommentDetailRespDto dto = BookCommentDetailRespDto.builder()
                .id(bookComment.getId())
                .commentContent(bookComment.getCommentContent())
                .replyCount(bookComment.getReplyCount())
                .commentUserId(bookComment.getUserId())
                .commentUser(userInfo != null ? userInfo.getUsername() : null)
                .commentUserPhoto(userInfo != null ? userInfo.getUserPhoto() : null)
                .commentTime(bookComment.getCreateTime())
                .build();

        return RestResp.ok(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConsts.USER_PURCHASE_CACHE_NAME,
            key = "#userId + ':' + #chapterId",
            cacheManager = CacheConsts.REDIS_CACHE_MANAGER)
    public RestResp<BookChapterBuyRespDto> buyBookVipChapter(Long chapterId) {
        // 1. 查询章节信息
        BookChapter bookChapter = bookChapterMapper.selectById(chapterId);
        if (bookChapter == null) {
            throw new BusinessException(ErrorCodeEnum.BOOK_CHAPTER_NOT_FOUND);
        }

        // 2. 检查章节是否为VIP章节
        if (bookChapter.getIsVip() == 0) {
            throw new BusinessException(ErrorCodeEnum.CHAPTER_NOT_VIP);
        }

        // 3. 获取当前用户ID
        Long userId = UserHolder.getUserId();

        // 4. 检查用户是否已购买过该章节
        QueryWrapper<UserConsumeLog> checkWrapper = new QueryWrapper<>();
        checkWrapper.lambda()
                .eq(UserConsumeLog::getUserId, userId)
                .eq(UserConsumeLog::getProductId, chapterId)
                .eq(UserConsumeLog::getProductType, 0);

        if (userConsumeLogMapper.selectCount(checkWrapper) > 0) {
            // 已购买过，直接返回章节内容
            BookContent bookContent = bookContentMapper.selectOne(
                    new QueryWrapper<BookContent>().lambda()
                            .eq(BookContent::getChapterId, chapterId)
            );

            UserInfo userInfo = userInfoMapper.selectById(userId);

            return RestResp.ok(BookChapterBuyRespDto.builder()
                    .accountBalance(userInfo.getAccountBalance())
                    .chapterContent(bookContent != null ? bookContent.getContent() : null)
                    .chapterName(bookChapter.getChapterName())
                    .build());
        }

        // 5. 查询小说信息以获取作家ID
        BookInfo bookInfo = bookInfoMapper.selectById(bookChapter.getBookId());
        if (bookInfo == null) {
            throw new BusinessException(ErrorCodeEnum.BOOK_NOT_FOUND);
        }

        // 6. 原子性扣除用户余额
        UpdateWrapper<UserInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda()
                .eq(UserInfo::getId, userId)
                .ge(UserInfo::getAccountBalance, VIP_CHAPTER_PRICE)
                .setSql("account_balance = account_balance - " + VIP_CHAPTER_PRICE)
                .set(UserInfo::getUpdateTime, LocalDateTime.now());

        boolean updateSuccess = userInfoMapper.update(null, updateWrapper) > 0;

        if (!updateSuccess) {
            throw new BusinessException(ErrorCodeEnum.USER_BALANCE_NOT_ENOUGH);
        }

        // 7. 记录消费日志
        UserConsumeLog consumeLog = new UserConsumeLog();
        consumeLog.setUserId(userId);
        consumeLog.setAmount(VIP_CHAPTER_PRICE);
        consumeLog.setProductType(0);
        consumeLog.setProductId(chapterId);
        consumeLog.setProducName(bookChapter.getChapterName());
        consumeLog.setProducValue(1);
        consumeLog.setCreateTime(LocalDateTime.now());
        consumeLog.setUpdateTime(LocalDateTime.now());
        userConsumeLogMapper.insert(consumeLog);

        // 8. 更新作家收入（新增）
        try {
            AuthorIncomeReqDto incomeReqDto = AuthorIncomeReqDto.builder()
                    .authorId(bookInfo.getAuthorId())
                    .bookId(bookChapter.getBookId())
                    .chapterId(chapterId)
                    .userPayAmount(VIP_CHAPTER_PRICE)
                    .incomeDate(LocalDate.now())
                    .build();

            // 使用异步方法更新作家收入，不影响主流程
            authorService.updateAuthorIncomeAsync(incomeReqDto);

            log.info("触发作家收入更新: authorId={}, bookId={}, chapterId={}, amount={}",
                    bookInfo.getAuthorId(), bookChapter.getBookId(), chapterId, VIP_CHAPTER_PRICE);
        } catch (Exception e) {
            // 作家收入更新失败不应影响主流程，只记录日志
            log.error("更新作家收入失败: {}", e.getMessage(), e);
        }


        // 9. 查询章节内容
        BookContent bookContent = bookContentMapper.selectOne(
                new QueryWrapper<BookContent>().lambda()
                        .eq(BookContent::getChapterId, chapterId)
        );

        // 10. 查询更新后的余额
        UserInfo updatedUserInfo = userInfoMapper.selectById(userId);

        // 11. 返回购买成功结果
        BookChapterBuyRespDto respDto = BookChapterBuyRespDto.builder()
                .accountBalance(updatedUserInfo.getAccountBalance())
                .chapterContent(bookContent != null ? bookContent.getContent() : null)
                .chapterName(bookChapter.getChapterName())
                .build();

        return RestResp.ok(respDto);
    }
}
