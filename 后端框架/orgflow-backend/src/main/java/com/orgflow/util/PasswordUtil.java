package com.orgflow.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类：统一使用 BCrypt 加密，数据库中不保存明文密码。
 */
public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    /** 明文加密 */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 校验明文与密文是否匹配。
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        // 兼容历史明文密码：如果库里存的不是 BCrypt 串，则直接比较
        if (!isBcrypt(encodedPassword)) {
            return rawPassword.equals(encodedPassword);
        }
        try {
            return ENCODER.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isBcrypt(String text) {
        return text != null && (text.startsWith("$2a$") || text.startsWith("$2b$") || text.startsWith("$2y$"));
    }

    /** 命令行生成密文用：java -cp ... PasswordUtil 123456 */
    public static void main(String[] args) {
        String raw = args.length > 0 ? args[0] : "123456";
        System.out.println(encode(raw));
    }
}
