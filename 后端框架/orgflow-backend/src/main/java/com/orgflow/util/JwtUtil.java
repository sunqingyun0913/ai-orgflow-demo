package com.orgflow.util;

import com.orgflow.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 轻量 JWT 工具类（HS256），不依赖任何第三方 JWT 库。
 *
 * <p>token 结构：Base64Url(header).Base64Url(payload).Base64Url(signature)</p>
 * <p>payload 中的自定义字段：userId、employeeId、role、username、iat、exp</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final JwtProperties jwtProperties;

    /** 缓存密钥字节，避免每次请求重复计算 */
    private volatile byte[] secretKeyBytes;

    /**
     * 启动时校验密钥配置。
     *
     * <p>JWT 的安全性完全取决于这个密钥：一旦密钥泄露（例如沿用仓库里的默认值），
     * 任何人都能离线签出 `{"userId":1,"role":1}` 的 token，直接拿到管理员权限。
     * 所以这里对「没配」「太短」「还是默认值」三种情况都打印醒目告警，
     * 但不直接启动失败——毕设本机演示要能一键跑起来。</p>
     */
    @jakarta.annotation.PostConstruct
    void checkSecret() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            log.error("========== 安全告警：orgflow.jwt.secret 未配置，JWT 无法签发出安全 token！==========");
            return;
        }
        if (secret.length() < 32) {
            log.warn("安全告警：JWT 密钥长度只有 {} 位，建议不少于 32 位", secret.length());
        }
        if (secret.contains("please-change-me") || secret.contains("default")) {
            log.warn("========== 安全告警：正在使用内置的 JWT 默认密钥，仅适合本机开发/答辩演示。"
                    + "部署到服务器前请设置环境变量 ORGFLOW_JWT_SECRET 为一串随机长字符串。==========");
        }
    }

    private byte[] keyBytes() {
        byte[] cached = this.secretKeyBytes;
        if (cached == null) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                cached = digest.digest(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new IllegalStateException("计算 JWT 密钥失败", e);
            }
            this.secretKeyBytes = cached;
        }
        return cached;
    }

    /**
     * 生成 token。
     *
     * @param userId     用户ID
     * @param employeeId 关联员工ID，可为 null
     * @param role       角色 1管理员 2普通员工
     * @param username   登录账号
     */
    public String createToken(Integer userId, Integer employeeId, Integer role, String username) {
        long now = System.currentTimeMillis();
        long expireAt = now + jwtProperties.getExpire();

        StringBuilder payload = new StringBuilder(256);
        payload.append('{');
        payload.append("\"userId\":").append(userId).append(',');
        payload.append("\"employeeId\":").append(employeeId == null ? "null" : employeeId).append(',');
        payload.append("\"role\":").append(role).append(',');
        payload.append("\"username\":\"").append(escape(username)).append("\",");
        payload.append("\"iat\":").append(now).append(',');
        payload.append("\"exp\":").append(expireAt);
        payload.append('}');

        String encodedHeader = encode(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = encode(payload.toString().getBytes(StandardCharsets.UTF_8));
        String content = encodedHeader + "." + encodedPayload;
        return content + "." + sign(content);
    }

    /**
     * 校验 token 是否有效（格式正确 + 签名匹配 + 未过期）。
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        String content = parts[0] + "." + parts[1];
        // 签名必须一致
        if (!constantTimeEquals(sign(content), parts[2])) {
            log.debug("JWT 签名校验失败");
            return false;
        }
        Long exp = getLong(token, "exp");
        if (exp == null) {
            return false;
        }
        if (System.currentTimeMillis() > exp) {
            log.debug("JWT 已过期");
            return false;
        }
        return true;
    }

    /** 取字符串字段，例如 username */
    public String getString(String token, String key) {
        String json = payloadJson(token);
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                .matcher(json);
        return matcher.find() ? unescape(matcher.group(1)) : null;
    }

    /** 取整数字段，例如 userId / role */
    public Integer getInteger(String token, String key) {
        Long value = getLong(token, key);
        return value == null ? null : value.intValue();
    }

    /** 取长整数字段，例如 exp / iat */
    public Long getLong(String token, String key) {
        String json = payloadJson(token);
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*(-?\\d+)")
                .matcher(json);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String payloadJson(String token) {
        if (token == null || token.isBlank()) {
            return "{}";
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return "{}";
        }
        try {
            return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "{}";
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(keyBytes(), HMAC_SHA256));
            return encode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名失败", e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 用固定时间比较，避免时序攻击 */
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unescape(String text) {
        return text.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
