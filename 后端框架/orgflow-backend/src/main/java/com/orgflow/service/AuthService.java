package com.orgflow.service;

import com.orgflow.dto.LoginDTO;
import com.orgflow.vo.LoginVO;
import com.orgflow.vo.UserVO;

/**
 * 认证服务
 */
public interface AuthService {

    /** 登录，成功返回 token 与用户信息 */
    LoginVO login(LoginDTO loginDTO);

    /** 查询当前登录用户信息 */
    UserVO getCurrentUserInfo(Integer userId);

    /** 修改自己的密码 */
    void updatePassword(Integer userId, String oldPassword, String newPassword);
}
