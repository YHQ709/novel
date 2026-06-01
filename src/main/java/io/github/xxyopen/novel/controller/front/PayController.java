package io.github.xxyopen.novel.controller.front;

import io.github.xxyopen.novel.core.auth.UserHolder;
import io.github.xxyopen.novel.core.common.constant.ErrorCodeEnum;
import io.github.xxyopen.novel.core.common.resp.RestResp;
import io.github.xxyopen.novel.core.constant.ApiRouterConsts;
import io.github.xxyopen.novel.dto.req.PayAlipayReqDto;
import io.github.xxyopen.novel.dto.resp.PayAlipayRespDto;
import io.github.xxyopen.novel.service.PayService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制器
 *
 * @author YHQ
 * @date 2026/2/23
 */
@Tag(name = "PayController", description = "前台门户-支付模块")
@RestController
@RequestMapping(ApiRouterConsts.API_FRONT_PAY_URL_PREFIX)
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    /**
     * 创建支付宝支付 - API接口，返回JSON
     */
    @PostMapping("/alipay/create")
    @ResponseBody
    public RestResp<PayAlipayRespDto> createAlipay(@RequestBody PayAlipayReqDto reqDto) {
        return payService.createAlipay(reqDto);
    }

    /**
     * 支付宝异步通知 - 支付宝服务器直接调用，返回纯文本
     */
    @PostMapping("/alipay/notify")
    @ResponseBody
    public String alipayNotify(HttpServletRequest request) {
        return payService.alipayNotify(request);
    }

    /**
     * 支付宝同步通知 - 页面跳转，返回重定向
     */
    @GetMapping("/alipay/return")
    public String alipayReturn(HttpServletRequest request) {
        return payService.alipayReturn(request);
    }

    /**
     * 支付结果页面
     */
    @GetMapping("/result")
    public String payResult(@RequestParam(required = false) Boolean success,
                            @RequestParam(required = false) String outTradeNo,
                            @RequestParam(required = false) String totalAmount,
                            @RequestParam(required = false) String msg,
                            org.springframework.ui.Model model) {
        model.addAttribute("success", success);
        model.addAttribute("outTradeNo", outTradeNo);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("msg", msg);
        return "pay/result";
    }
}
