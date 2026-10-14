package com.orgflow.vo;

import lombok.Data;

/**
 * 登录成功返回数据
 */
@Data
public class LoginVO {

    /** JWT token，前端放 localStorage，请求时带 Authorization: Bearer <token> */
    private String token;

    /** 用户账号信息（含关联员工信息） */
    private UserVO user;

    /** 当前登录人姓名，方便前端打招呼 */
    private String displayName;
}
