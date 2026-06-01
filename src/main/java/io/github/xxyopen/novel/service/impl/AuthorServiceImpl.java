package io.github.xxyopen.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.xxyopen.novel.core.auth.UserHolder;
import io.github.xxyopen.novel.core.common.constant.ErrorCodeEnum;
import io.github.xxyopen.novel.core.common.req.PageReqDto;
import io.github.xxyopen.novel.core.common.resp.PageRespDto;
import io.github.xxyopen.novel.core.common.resp.RestResp;
import io.github.xxyopen.novel.core.constant.DatabaseConsts;
import io.github.xxyopen.novel.dao.entity.AuthorIncome;
import io.github.xxyopen.novel.dao.entity.AuthorIncomeDetail;
import io.github.xxyopen.novel.dao.entity.AuthorInfo;
import io.github.xxyopen.novel.dao.entity.BookChapter;
import io.github.xxyopen.novel.dao.mapper.AuthorIncomeDetailMapper;
import io.github.xxyopen.novel.dao.mapper.AuthorIncomeMapper;
import io.github.xxyopen.novel.dao.mapper.AuthorInfoMapper;
import io.github.xxyopen.novel.dto.AuthorInfoDto;
import io.github.xxyopen.novel.dto.req.AuthorIncomeDetailReqDto;
import io.github.xxyopen.novel.dto.req.AuthorIncomeReqDto;
import io.github.xxyopen.novel.dto.req.AuthorRegisterReqDto;
import io.github.xxyopen.novel.dto.resp.AuthorIncomeDetailRespDto;
import io.github.xxyopen.novel.dto.resp.AuthorIncomeRespDto;
import io.github.xxyopen.novel.dto.resp.BookChapterRespDto;
import io.github.xxyopen.novel.manager.cache.AuthorInfoCacheManager;
import io.github.xxyopen.novel.service.AuthorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 作家模块 服务实现类
 *
 * @author xiongxiaoyang
 * @date 2022/5/23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorServiceImpl implements AuthorService {

    private final AuthorInfoCacheManager authorInfoCacheManager;

    private final AuthorInfoMapper authorInfoMapper;

    private  final AuthorIncomeMapper authorIncomeMapper;

    private final AuthorIncomeDetailMapper authorIncomeDetailMapper;

    // 作家分成比例常量
    private static final BigDecimal AUTHOR_SHARE_RATIO = new BigDecimal("0.85");

    // 税率常量 - 14%
    private static final BigDecimal TAX_RATE = new BigDecimal("0.14");

    // 支付状态常量
    private static final int PAY_STATUS_PENDING = 0;

    // 确认状态常量
    private static final int CONFIRM_STATUS_PENDING = 0;

    @Override
    public RestResp<Void> register(AuthorRegisterReqDto dto) {
        // 校验该用户是否已注册为作家
        AuthorInfoDto author = authorInfoCacheManager.getAuthor(dto.getUserId());
        if (Objects.nonNull(author)) {
            // 该用户已经是作家，直接返回
            return RestResp.ok();
        }
        // 保存作家注册信息
        AuthorInfo authorInfo = new AuthorInfo();
        authorInfo.setUserId(dto.getUserId());
        authorInfo.setChatAccount(dto.getChatAccount());
        authorInfo.setEmail(dto.getEmail());
        authorInfo.setInviteCode("0");
        authorInfo.setTelPhone(dto.getTelPhone());
        authorInfo.setPenName(dto.getPenName());
        authorInfo.setWorkDirection(dto.getWorkDirection());
        authorInfo.setCreateTime(LocalDateTime.now());
        authorInfo.setUpdateTime(LocalDateTime.now());
        authorInfoMapper.insert(authorInfo);
        // 清除作家缓存
        authorInfoCacheManager.evictAuthorCache();
        return RestResp.ok();
    }

    @Override
    public RestResp<Integer> getStatus(Long userId) {
        AuthorInfoDto author = authorInfoCacheManager.getAuthor(userId);
        return Objects.isNull(author) ? RestResp.ok(null) : RestResp.ok(author.getStatus());
    }

    @Override
    public RestResp<PageRespDto<AuthorIncomeDetailRespDto>> listAuthorSubscribeDetails(PageReqDto dto) {
        IPage<AuthorIncomeDetail> page = new Page<>();
        page.setCurrent(dto.getPageNum());
        page.setSize(dto.getPageSize());
        QueryWrapper<AuthorIncomeDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("author_id", UserHolder.getAuthorId())
                .orderByDesc("income_date");
        IPage<AuthorIncomeDetail> incomeDetailPage = authorIncomeDetailMapper.selectPage(page, queryWrapper);
        return RestResp.ok(PageRespDto.of(dto.getPageNum(), dto.getPageSize(), page.getTotal(),
                incomeDetailPage.getRecords().stream().map(v -> AuthorIncomeDetailRespDto.builder()
                        .id(v.getId())
                        .incomeDate(v.getIncomeDate())
                        .incomeAccount(v.getIncomeAccount())
                        .incomeCount(v.getIncomeCount())
                        .incomeNumber(v.getIncomeNumber())
                        .build()).toList()));
    }

    @Override
    public RestResp<PageRespDto<AuthorIncomeRespDto>> listAuthorIncomes(PageReqDto dto) {
        IPage<AuthorIncome> page = new Page<>();
        page.setCurrent(dto.getPageNum());
        page.setSize(dto.getPageSize());
        QueryWrapper<AuthorIncome> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("author_id", UserHolder.getAuthorId())
                .orderByDesc("income_month");
        IPage<AuthorIncome> incomePage = authorIncomeMapper.selectPage(page, queryWrapper);
        return RestResp.ok(PageRespDto.of(dto.getPageNum(), dto.getPageSize(), page.getTotal(),
                incomePage.getRecords().stream().map(v -> AuthorIncomeRespDto.builder()
                        .id(v.getId())
                        .incomeMonth(v.getIncomeMonth())
                        .preTaxIncome(v.getPreTaxIncome())
                        .afterTaxIncome(v.getAfterTaxIncome())
                        .payStatus(v.getPayStatus())
                        .build()).toList()));
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CompletableFuture<RestResp<Void>> updateAuthorIncomeAsync(AuthorIncomeReqDto dto) {
        try {
            // 1. 计算税前收入（用户支付金额 × 90%）
            BigDecimal userPay = new BigDecimal(dto.getUserPayAmount());
            Integer preTaxIncome = userPay.multiply(AUTHOR_SHARE_RATIO).intValue();

            // 2. 计算税后收入（税前收入 × (1 - 14%)）
            Integer afterTaxIncome = new BigDecimal(preTaxIncome)
                    .multiply(BigDecimal.ONE.subtract(TAX_RATE))
                    .intValue();

            // 3. 更新日明细表
            updateIncomeDetail(dto, preTaxIncome);

            // 4. 更新月统计表（只需要总和，不需要detail字段）
            updateIncomeSummary(dto, preTaxIncome, afterTaxIncome);

            return CompletableFuture.completedFuture(RestResp.ok());
        } catch (Exception e) {
            log.error("更新作家收入失败", e);
            return CompletableFuture.completedFuture(RestResp.fail(ErrorCodeEnum.INCOME_UPDATE_FAIL));
        }
    }

    /**
     * 更新日明细表
     */
    private void updateIncomeDetail(AuthorIncomeReqDto dto, Integer preTaxIncome) {
        QueryWrapper<AuthorIncomeDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(AuthorIncomeDetail::getAuthorId, dto.getAuthorId())
                .eq(AuthorIncomeDetail::getBookId, dto.getBookId())
                .eq(AuthorIncomeDetail::getIncomeDate, dto.getIncomeDate());

        AuthorIncomeDetail detail = authorIncomeDetailMapper.selectOne(queryWrapper);

        if (detail == null) {
            // 新增
            detail = new AuthorIncomeDetail();
            detail.setAuthorId(dto.getAuthorId());
            detail.setBookId(dto.getBookId());
            detail.setIncomeDate(dto.getIncomeDate());
            detail.setIncomeAccount(preTaxIncome);
            detail.setIncomeCount(1);
            detail.setIncomeNumber(1);
            detail.setCreateTime(LocalDateTime.now());
            detail.setUpdateTime(LocalDateTime.now());
            authorIncomeDetailMapper.insert(detail);
        } else {
            // 更新
            UpdateWrapper<AuthorIncomeDetail> updateWrapper = new UpdateWrapper<>();
            updateWrapper.lambda()
                    .eq(AuthorIncomeDetail::getId, detail.getId())
                    .setSql("income_account = income_account + " + preTaxIncome)
                    .setSql("income_count = income_count + 1")
                    .setSql("income_number = income_number + 1")
                    .set(AuthorIncomeDetail::getUpdateTime, LocalDateTime.now());
            authorIncomeDetailMapper.update(null, updateWrapper);
        }
    }

    /**
     * 更新月统计表（简化版，不需要detail字段）
     */
    private void updateIncomeSummary(AuthorIncomeReqDto dto, Integer preTaxIncome, Integer afterTaxIncome) {
        LocalDate incomeMonth = dto.getIncomeDate().withDayOfMonth(1);

        QueryWrapper<AuthorIncome> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(AuthorIncome::getAuthorId, dto.getAuthorId())
                .eq(AuthorIncome::getBookId, dto.getBookId())
                .eq(AuthorIncome::getIncomeMonth, incomeMonth);

        AuthorIncome income = authorIncomeMapper.selectOne(queryWrapper);

        if (income == null) {
            // 新增 - 不需要设置detail
            income = new AuthorIncome();
            income.setAuthorId(dto.getAuthorId());
            income.setBookId(dto.getBookId());
            income.setIncomeMonth(incomeMonth);
            income.setPreTaxIncome(preTaxIncome);
            income.setAfterTaxIncome(afterTaxIncome);
            income.setPayStatus(0);
            income.setConfirmStatus(0);
            // detail字段可以为null或空字符串
            income.setDetail("{}");
            income.setCreateTime(LocalDateTime.now());
            income.setUpdateTime(LocalDateTime.now());
            authorIncomeMapper.insert(income);
        } else {
            // 更新 - 不需要更新detail
            UpdateWrapper<AuthorIncome> updateWrapper = new UpdateWrapper<>();
            updateWrapper.lambda()
                    .eq(AuthorIncome::getId, income.getId())
                    .setSql("pre_tax_income = pre_tax_income + " + preTaxIncome)
                    .setSql("after_tax_income = after_tax_income + " + afterTaxIncome)
                    .set(AuthorIncome::getUpdateTime, LocalDateTime.now());
            authorIncomeMapper.update(null, updateWrapper);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public RestResp<Void> updateAuthorIncomeDetail(AuthorIncomeDetailReqDto dto) {
        log.info("开始更新作家收入明细: {}", dto);

        try {
            // 参数校验
            if (dto == null || dto.getAuthorId() == null || dto.getBookId() == null
                    || dto.getIncomeDate() == null || dto.getIncomeAccount() == null) {
                log.error("更新作家收入明细参数不完整: {}", dto);
                return RestResp.fail(ErrorCodeEnum.USER_REQUEST_PARAM_ERROR);
            }

            // 查询当日是否已有收入明细记录
            QueryWrapper<AuthorIncomeDetail> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda()
                    .eq(AuthorIncomeDetail::getAuthorId, dto.getAuthorId())
                    .eq(AuthorIncomeDetail::getBookId, dto.getBookId())
                    .eq(AuthorIncomeDetail::getIncomeDate, dto.getIncomeDate());

            AuthorIncomeDetail detail = authorIncomeDetailMapper.selectOne(queryWrapper);

            if (detail == null) {
                // 新增收入明细记录
                detail = new AuthorIncomeDetail();
                detail.setAuthorId(dto.getAuthorId());
                detail.setBookId(dto.getBookId());
                detail.setIncomeDate(dto.getIncomeDate());
                detail.setIncomeAccount(dto.getIncomeAccount());
                detail.setIncomeCount(dto.getIncomeCount() != null ? dto.getIncomeCount() : 1);
                detail.setIncomeNumber(dto.getIncomeNumber() != null ? dto.getIncomeNumber() : 1);
                detail.setCreateTime(LocalDateTime.now());
                detail.setUpdateTime(LocalDateTime.now());

                int inserted = authorIncomeDetailMapper.insert(detail);
                if (inserted > 0) {
                    log.info("新增作家收入明细成功: id={}", detail.getId());
                    return RestResp.ok();
                } else {
                    log.error("新增作家收入明细失败");
                    return RestResp.fail(ErrorCodeEnum.AUTHOR_INCOME_DETAIL_UPDATE_FAIL);
                }
            } else {
                // 更新现有收入明细记录
                UpdateWrapper<AuthorIncomeDetail> updateWrapper = new UpdateWrapper<>();
                updateWrapper.lambda()
                        .eq(AuthorIncomeDetail::getId, detail.getId())
                        .setSql("income_account = income_account + " + dto.getIncomeAccount())
                        .set(AuthorIncomeDetail::getUpdateTime, LocalDateTime.now());

                // 如果有传入订阅次数和人数，也进行累加
                if (dto.getIncomeCount() != null) {
                    updateWrapper.lambda().setSql("income_count = income_count + " + dto.getIncomeCount());
                }
                if (dto.getIncomeNumber() != null) {
                    updateWrapper.lambda().setSql("income_number = income_number + " + dto.getIncomeNumber());
                }

                int updated = authorIncomeDetailMapper.update(null, updateWrapper);
                if (updated > 0) {
                    log.info("更新作家收入明细成功: id={}", detail.getId());
                    return RestResp.ok();
                } else {
                    log.error("更新作家收入明细失败: id={}", detail.getId());
                    return RestResp.fail(ErrorCodeEnum.AUTHOR_INCOME_DETAIL_UPDATE_FAIL);
                }
            }
        } catch (Exception e) {
            log.error("更新作家收入明细异常: {}", e.getMessage(), e);
            return RestResp.fail(ErrorCodeEnum.AUTHOR_INCOME_DETAIL_UPDATE_FAIL);
        }
    }


}
