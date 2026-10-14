package com.orgflow.common;

import lombok.Getter;

/**
 * 业务异常。抛出后会被 {@link GlobalExceptionHandler} 捕获，
 * 并按其中的 code 返回给前端（不打印堆栈）。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        this(ResultCode.BAD_REQUEST.getCode(), message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMessage());
    }

    /** 参数错误 400 */
    public static BusinessException badRequest(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /** 未登录/登录失效 401 */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(ResultCode.UNAUTHORIZED.getCode(), message);
    }

    /** 无权限 403 */
    public static BusinessException forbidden(String message) {
        return new BusinessException(ResultCode.FORBIDDEN.getCode(), message);
    }

    /** 资源不存在 404 */
    public static BusinessException notFound(String message) {
        return new BusinessException(ResultCode.NOT_FOUND.getCode(), message);
    }
}
