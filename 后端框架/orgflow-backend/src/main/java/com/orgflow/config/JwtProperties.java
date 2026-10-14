package com.orgflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 相关配置，对应 application.yml 中的 orgflow.jwt.*
 */
@Data
@Component
@ConfigurationProperties(prefix = "orgflow.jwt")
public class JwtProperties {

    /**
     * 签名密钥。默认值来自 application.yml 的 ${ORGFLOW_JWT_SECRET:...}，
     * 部署时应通过环境变量 ORGFLOW_JWT_SECRET 覆盖；使用内置默认值时会打印醒目告警。
     * 这里不提供兜底默认值，避免「忘记配置也能跑起来」导致密钥变成公开常量。
     */
    private String secret;

    /** token 有效期（毫秒） */
    private Long expire = 604800000L;
}
