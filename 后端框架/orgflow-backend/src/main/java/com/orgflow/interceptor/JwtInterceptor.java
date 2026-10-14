package com.orgflow.interceptor;

import com.orgflow.common.BusinessException;
import com.orgflow.entity.User;
import com.orgflow.mapper.UserMapper;
import com.orgflow.util.CurrentUserUtil;
import com.orgflow.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器：需要登录的接口必须带请求头
 * {@code Authorization: Bearer <token>}。
 *
 * <p><b>为什么每次请求都要回查数据库：</b>token 里的 role/status 是「登录那一刻的快照」，
 * 而 token 有效期是 7 天。如果只信 token，那么管理员把某个账号禁用、降权或删除之后，
 * 该账号的旧 token 在 7 天内依然能按旧角色操作（例如被降权的人仍能以管理员身份删数据），
 * 权限变更为「立即可生效」就成了一句空话。这里按主键查一次用户（成本很低），
 * 以数据库中的 status 和 role 为准。</p>
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    /** 启用状态 */
    private static final int STATUS_ENABLED = 1;

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 跨域预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            throw BusinessException.unauthorized("未登录，请先登录");
        }
        String token = header.substring(TOKEN_PREFIX.length()).trim();
        if (!jwtUtil.validateToken(token)) {
            throw BusinessException.unauthorized("登录已失效，请重新登录");
        }

        Integer userId = jwtUtil.getInteger(token, "userId");
        if (userId == null) {
            throw BusinessException.unauthorized("登录信息不完整，请重新登录");
        }

        // 回查数据库：用户被删除 → 401；被禁用 → 403；角色以库中当前值为准
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.unauthorized("账号不存在或已被删除，请重新登录");
        }
        if (!Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            throw BusinessException.forbidden("该账号已被禁用，请联系管理员");
        }

        request.setAttribute(CurrentUserUtil.ATTR_USER_ID, user.getId());
        request.setAttribute(CurrentUserUtil.ATTR_EMPLOYEE_ID, user.getEmployeeId());
        // 用库里的 role，而不是 token 里的快照
        request.setAttribute(CurrentUserUtil.ATTR_ROLE, user.getRole());
        request.setAttribute(CurrentUserUtil.ATTR_USERNAME, user.getUsername());
        return true;
    }
}
