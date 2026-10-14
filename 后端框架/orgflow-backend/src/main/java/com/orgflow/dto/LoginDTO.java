package com.orgflow.dto;

import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginDTO {

    /** 登录账号 */
    private String username;

    /** 登录密码（明文，后端用 BCrypt 校验） */
    private String password;
}
