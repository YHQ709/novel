package io.github.xxyopen.novel.core.config;


import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ：YHQ
 * @date ：2026/2/23
 * @description：alipay沙箱环境配置
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    /** 应用ID */
    private String appId;

    /** 应用私钥 */
    private String privateKey;

    /** 支付宝公钥 */
    private String alipayPublicKey;

    /** 异步通知地址 */
    private String notifyUrl;

    /** 同步通知地址 */
    private String returnUrl;

    /** 签名类型 */
    private String signType;

    /** 字符编码 */
    private String charset;

    /** 支付宝网关 */
    private String gatewayUrl;



    /**
     * 初始化AlipayClient
     * AlipayClient是线程安全的，可以单例使用
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                // 支付宝网关
                gatewayUrl,
                // 应用ID
                appId,
                // 应用私钥
                privateKey,
                // 数据格式
                "json",
                // 字符编码
                charset,
                // 支付宝公钥
                alipayPublicKey,
                // 签名算法
                signType
        );
    }
}
