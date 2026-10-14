package com.orgflow.util;

import com.orgflow.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 当前登录用户工具类。
 *
 * <p>{@link com.orgflow.interceptor.JwtInterceptor} 校验 token 后会把
 * userId / employeeId / role / username 放进 request attribute，
 * Controller 里统一用本类获取当前用户。</p>
 */
public final class CurrentUserUtil {

    public static final String ATTR_USER_ID = "currentUserId";
    public static final String ATTR_EMPLOYEE_ID = "currentEmployeeId";
    public static final String ATTR_ROLE = "currentRole";
    public static final String ATTR_USERNAME = "currentUsername";

    /** 管理员角色 */
    public static final int ROLE_ADMIN = 1;
    /** 普通员工角色 */
    public static final int ROLE_EMPLOYEE = 2;

    private CurrentUserUtil() {
    }

    /** 当前用户ID（user 表主键），未登录时抛 401 */
    public static Integer getUserId(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_USER_ID);
        if (value == null) {
            throw BusinessException.unauthorized("未登录或登录已失效");
        }
        return (Integer) value;
    }

    /** 当前用户关联的员工ID，可能为 null */
    public static Integer getEmployeeId(HttpServletRequest request) {
        return (Integer) request.getAttribute(ATTR_EMPLOYEE_ID);
    }

    /** 当前用户角色：1管理员 2普通员工 */
    public static Integer getRole(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_ROLE);
        if (value == null) {
            throw BusinessException.unauthorized("未登录或登录已失效");
        }
        return (Integer) value;
    }

    public static String getUsername(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_USERNAME);
        return value == null ? null : value.toString();
    }

    /** 是否管理员 */
    public static boolean isAdmin(HttpServletRequest request) {
        return Integer.valueOf(ROLE_ADMIN).equals(getRole(request));
    }

    /** 要求必须是管理员，否则抛 403 */
    public static void requireAdmin(HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw BusinessException.forbidden("该操作仅管理员可用");
        }
    }
}
