package io.github.xxyopen.novel.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.xxyopen.novel.core.auth.UserHolder;
import io.github.xxyopen.novel.core.common.constant.ErrorCodeEnum;
import io.github.xxyopen.novel.core.common.exception.BusinessException;
import io.github.xxyopen.novel.core.common.resp.RestResp;
import io.github.xxyopen.novel.core.config.AlipayConfig;
import io.github.xxyopen.novel.dao.entity.PayAlipay;
import io.github.xxyopen.novel.dao.entity.UserInfo;
import io.github.xxyopen.novel.dao.entity.UserPayLog;
import io.github.xxyopen.novel.dao.mapper.PayAlipayMapper;
import io.github.xxyopen.novel.dao.mapper.UserInfoMapper;
import io.github.xxyopen.novel.dao.mapper.UserPayLogMapper;
import io.github.xxyopen.novel.dto.req.PayAlipayReqDto;
import io.github.xxyopen.novel.dto.resp.PayAlipayRespDto;
import io.github.xxyopen.novel.service.PayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 支付服务实现类
 *
 * @author YHQ
 * @date 2026/2/23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayServiceImpl implements PayService {

    private final AlipayConfig alipayConfig;
    private final AlipayClient alipayClient;
    private final PayAlipayMapper payAlipayMapper;
    private final UserPayLogMapper userPayLogMapper;
    private final UserInfoMapper userInfoMapper;

    /**
     * 汇率：1元 = 100星币
     */
    private static final int EXCHANGE_RATE = 100;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RestResp createAlipay(PayAlipayReqDto reqDto) {
        try {
            // 1. 生成商户订单号
            String outTradeNo = generateOutTradeNo();
            // 2. 计算金额（元转分）
            Integer totalAmount = reqDto.getAmount() * 100;
            // 3. 计算星币数量
            Integer productValue = reqDto.getAmount() * EXCHANGE_RATE;
            reqDto.setProductValue(productValue);
            // 4. 创建支付宝支付请求
            AlipayTradePagePayRequest request = getAlipayTradePagePayRequest(reqDto, outTradeNo);
            // 5. 调用支付宝SDK生成表单
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (!response.isSuccess()) {
                // 创建支付宝支付失败
                throw new BusinessException(ErrorCodeEnum.PAY_CREATE_ERROR);
            }
            // 6. 保存支付记录到支付宝支付表
            PayAlipay payAlipay = new PayAlipay();
            payAlipay.setOutTradeNo(outTradeNo);
            payAlipay.setTotalAmount(totalAmount);
            payAlipay.setTradeStatus("WAIT_BUYER_PAY");
            payAlipay.setGmtCreate(LocalDateTime.now());
            payAlipay.setCreateTime(LocalDateTime.now());
            payAlipay.setUpdateTime(LocalDateTime.now());
            payAlipayMapper.insert(payAlipay);
            // 7. 保存用户支付日志
            UserPayLog userPayLog = new UserPayLog();
            userPayLog.setUserId(UserHolder.getUserId());
            userPayLog.setPayChannel(reqDto.getPayChannel());
            userPayLog.setOutTradeNo(outTradeNo);
            userPayLog.setAmount(totalAmount);
            userPayLog.setProductType(reqDto.getProductType());
            userPayLog.setProductId(reqDto.getProductId());
            userPayLog.setProductName(reqDto.getProductName());
            userPayLog.setProductValue(reqDto.getProductValue());
            userPayLog.setPayTime(LocalDateTime.now());
            userPayLog.setCreateTime(LocalDateTime.now());
            userPayLog.setUpdateTime(LocalDateTime.now());
            userPayLogMapper.insert(userPayLog);
            // 8. 返回支付表单
            return RestResp.ok(PayAlipayRespDto.builder()
                    .payForm(response.getBody())
                    .outTradeNo(outTradeNo)
                    .totalAmount(totalAmount)
                    .build());
        } catch (AlipayApiException e) {
            // 支付宝支付异常
            throw new BusinessException(ErrorCodeEnum.PAY_CREATE_ERROR);
        }
    }

    private @NotNull AlipayTradePagePayRequest getAlipayTradePagePayRequest(PayAlipayReqDto reqDto, String outTradeNo) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnUrl());
        // 设置业务参数
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(outTradeNo);
        model.setTotalAmount(new BigDecimal(reqDto.getAmount()).toString());
        model.setSubject(reqDto.getProductName() + "充值");
        model.setBody(reqDto.getProductValue() + "星币");
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        request.setBizModel(model);
        return request;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String alipayNotify(HttpServletRequest request) {
        String outTradeNo = "unknown";
        try {
            log.info("========== 收到支付宝异步通知 ==========");
            // 1. 获取所有参数
            Map<String, String> params = getAlipayParams(request);
            outTradeNo = params.getOrDefault("out_trade_no", "unknown");
            log.info("订单号: {}, 交易状态: {}", outTradeNo, params.get("trade_status"));

            // 2. 验证签名
            boolean signVerified = false;
            try {
                if (params.isEmpty()) {
                    throw new AlipayApiException("待验签内容不可为空");
                }
                if (!params.containsKey("sign")) {
                    throw new AlipayApiException("缺少sign参数");
                }
                String sign = params.get("sign");
                if (sign == null || sign.trim().isEmpty()) {
                    throw new AlipayApiException("sign参数为空");
                }
                signVerified = AlipaySignature.rsaCheckV1(
                        params,
                        alipayConfig.getAlipayPublicKey(),
                        alipayConfig.getCharset(),
                        alipayConfig.getSignType()
                );
            } catch (AlipayApiException e) {
                return "failure";
            }

            if (!signVerified) {
                return "failure";
            }

            // 3. 获取交易状态
            String tradeStatus = params.get("trade_status");
            String tradeNo = params.get("trade_no");

            // 4. 查询支付记录
            QueryWrapper wrapper = new QueryWrapper<>();
            wrapper.eq("out_trade_no", outTradeNo);
            PayAlipay payAlipay = payAlipayMapper.selectOne(wrapper);
            if (payAlipay == null) {
                return "failure";
            }

            // 5. 更新支付宝支付记录
            payAlipay.setTradeNo(tradeNo);
            payAlipay.setBuyerId(params.get("buyer_id"));
            payAlipay.setTradeStatus(tradeStatus);
            String receiptAmount = params.get("receipt_amount");
            if (receiptAmount != null && !receiptAmount.isEmpty()) {
                payAlipay.setReceiptAmount(new BigDecimal(receiptAmount).multiply(new BigDecimal(100)).intValue());
            }
            String gmtPayment = params.get("gmt_payment");
            if (gmtPayment != null && !gmtPayment.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                payAlipay.setGmtPayment(LocalDateTime.parse(gmtPayment, formatter));
            }
            payAlipay.setUpdateTime(LocalDateTime.now());
            payAlipayMapper.updateById(payAlipay);

            // 6. 交易成功处理
            if ("TRADE_SUCCESS".equals(tradeStatus)) {
                log.info("开始处理成功订单: {}", outTradeNo);
                QueryWrapper logWrapper = new QueryWrapper<>();
                logWrapper.eq("out_trade_no", outTradeNo);
                UserPayLog userPayLog = userPayLogMapper.selectOne(logWrapper);
                if (userPayLog == null) {
                    log.error("未找到支付日志, outTradeNo: {}", outTradeNo);
                    return "failure";
                }
                log.info("用户ID: {}, 应增加星币: {}", userPayLog.getUserId(), userPayLog.getProductValue());

                UserInfo userInfo = userInfoMapper.selectById(userPayLog.getUserId());
                if (userInfo == null) {
                    log.error("未找到用户信息, userId: {}", userPayLog.getUserId());
                    return "failure";
                }

                Long oldBalance = userInfo.getAccountBalance();
                Long addAmount = Long.valueOf(userPayLog.getProductValue());
                Long newBalance = oldBalance + addAmount;
                log.info("余额更新: {} -> {}", oldBalance, newBalance);

                userInfo.setAccountBalance(newBalance);
                userInfo.setUpdateTime(LocalDateTime.now());
                userInfoMapper.updateById(userInfo);

                userPayLog.setPayTime(LocalDateTime.now());
                userPayLog.setUpdateTime(LocalDateTime.now());
                userPayLogMapper.updateById(userPayLog);
                log.info("订单处理完成: {}", outTradeNo);
            }

            return "success";
        } catch (Exception e) {
            log.error("支付宝回调处理失败, outTradeNo: {}", outTradeNo, e);
            return "failure";
        }
    }

    @Override
    public String alipayReturn(HttpServletRequest request) {
        try {
            Map<String, String> params = getAlipayParams(request);
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );
            if (!signVerified) {
                return "redirect:/pay/result?success=false&msg=签名验证失败";
            }
            String outTradeNo = params.get("out_trade_no");
            String totalAmount = params.get("total_amount");
            return "redirect:/pay/result?success=true&outTradeNo=" + outTradeNo + "&totalAmount=" + totalAmount;
        } catch (Exception e) {
            return "redirect:/pay/result?success=false&msg=系统异常";
        }
    }

    /**
     * 生成商户订单号
     * 格式：yyyyMMddHHmmss + 8位随机数
     */
    private String generateOutTradeNo() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timeStr = now.format(formatter);
        String randomStr = String.format("%08d", new Random().nextInt(100000000));
        return timeStr + randomStr;
    }

    /**
     * 获取支付宝参数
     */
    private Map<String, String> getAlipayParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();

        // 1. 首先从 URL 参数中获取（同步通知使用）
        Map<String, String[]> requestParams = request.getParameterMap();
        if (requestParams != null && !requestParams.isEmpty()) {
            for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
                String name = entry.getKey();
                String[] values = entry.getValue();
                String valueStr = String.join(",", values);
                params.put(name, valueStr);
            }
        }

        // 2. 如果 URL 参数为空，尝试从请求体读取（异步通知使用）
        if (params.isEmpty()) {
            try {
                BufferedReader reader = request.getReader();
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String body = sb.toString();

                if (body != null && !body.isEmpty()) {
                    String[] pairs = body.split("&");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=");
                        if (keyValue.length == 2) {
                            try {
                                String key = keyValue[0];
                                String value = URLDecoder.decode(keyValue[1], "UTF-8");
                                params.put(key, value);
                            } catch (Exception e) {
                                // 忽略解码异常
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略读取异常
            }
        }

        return params;
    }
}