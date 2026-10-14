package com.orgflow.controller;

import com.orgflow.common.Result;
import com.orgflow.dto.LoginDTO;
import com.orgflow.dto.PasswordDTO;
import com.orgflow.service.AuthService;
import com.orgflow.util.CurrentUserUtil;
import com.orgflow.vo.LoginVO;
import com.orgflow.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录、获取当前用户、退出登录、修改密码。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录（免登录接口） */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        return Result.success("登录成功", authService.login(loginDTO));
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/info")
    public Result<UserVO> info(HttpServletRequest request) {
        return Result.success(authService.getCurrentUserInfo(CurrentUserUtil.getUserId(request)));
    }

    /**
     * 退出登录。
     * JWT 是无状态的，服务端不保存会话，前端删除本地 token 即可。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("已退出登录", null);
    }

    /** 修改当前用户密码 */
    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody PasswordDTO passwordDTO, HttpServletRequest request) {
        authService.updatePassword(CurrentUserUtil.getUserId(request),
                passwordDTO.getOldPassword(), passwordDTO.getNewPassword());
        return Result.success("密码修改成功", null);
    }
}
