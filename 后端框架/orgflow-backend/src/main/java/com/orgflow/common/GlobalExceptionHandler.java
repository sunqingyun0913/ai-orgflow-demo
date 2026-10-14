package com.orgflow.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 全局异常处理器：把各种异常统一转换成 {@link Result} 返回，
 * 前端永远只需要处理 {code, message, data} 这一种结构。
 *
 * <p><b>重要</b>：契约第 2 节写的是「code 与 HTTP 状态码一致」，所以除了返回业务 code，
 * 还要用 {@link ResponseStatus} 让 HTTP 状态码也对齐。这样网关、监控、非浏览器客户端
 * 才能靠状态码判断成败，而不是只能解析响应体。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：可预期，只记 warn，不打堆栈。
     * HTTP 状态码动态取业务 code，因此这里不能用固定的 {@code @ResponseStatus}，
     * 而是直接返回 {@link org.springframework.http.ResponseEntity}。
     */
    @ExceptionHandler(BusinessException.class)
    public org.springframework.http.ResponseEntity<Result<Void>> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{} {}] code={} message={}",
                request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage());
        HttpStatus status = resolveStatus(e.getCode());
        return org.springframework.http.ResponseEntity.status(status)
                .body(Result.error(e.getCode(), e.getMessage()));
    }

    /** 业务 code 转 HTTP 状态码；不认识的 code 一律按 400 处理，避免出现非法状态码 */
    private static HttpStatus resolveStatus(Integer code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST;
        }
        HttpStatus status = HttpStatus.resolve(code);
        return (status == null || !status.isError()) ? HttpStatus.BAD_REQUEST : status;
    }

    /** @Valid 校验失败（当前 DTO 未使用校验注解，保留此分支以便后续接入 spring-boot-starter-validation） */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidException(BindException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String message = fieldErrors.isEmpty()
                ? ResultCode.BAD_REQUEST.getMessage()
                : fieldErrors.get(0).getField() + " " + fieldErrors.get(0).getDefaultMessage();
        return Result.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /** 缺少必填请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(ResultCode.BAD_REQUEST.getCode(), "缺少必填参数：" + e.getParameterName());
    }

    /** 参数类型不匹配，例如 /api/department/abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.error(ResultCode.BAD_REQUEST.getCode(), "参数格式不正确：" + e.getName());
    }

    /**
     * 请求体不是合法 JSON 或字段类型不匹配，例如 {"username":"admin","password":123}。
     *
     * <p>必须单独处理：否则会落到兜底分支返回 500（契约要求 400），而且 Spring 会把
     * Jackson 的原始异常信息（可能包含请求体片段，例如明文密码）带进日志。</p>
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        // 只记录接口位置，不打印 e.getMessage()，避免把敏感请求体写进日志
        log.warn("请求体解析失败 [{} {}]", request.getMethod(), request.getRequestURI());
        return Result.error(ResultCode.BAD_REQUEST.getCode(), "请求参数格式不正确，请检查提交的 JSON");
    }

    /** 请求方式不支持，例如用 GET 访问只支持 POST 的接口 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(405, "请求方式不支持：" + e.getMethod());
    }

    /** 静态资源/接口不存在 */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        return Result.error(ResultCode.NOT_FOUND.getCode(), "接口不存在：" + e.getResourcePath());
    }

    /** 兜底：未预料到的异常，必须打印堆栈方便排查，但只给前端返回固定文案 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{} {}]", request.getMethod(), request.getRequestURI(), e);
        return Result.error(ResultCode.ERROR.getCode(), "服务器内部错误，请联系管理员");
    }
}
