package com.orgflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orgflow.common.Result;
import com.orgflow.dto.UserDTO;
import com.orgflow.service.UserService;
import com.orgflow.util.CurrentUserUtil;
import com.orgflow.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户账号管理接口（契约 4.5）。
 *
 * <p>整个模块仅管理员可用：每个方法的第一行都调用
 * {@link CurrentUserUtil#requireAdmin(HttpServletRequest)}，
 * 非管理员直接 403。返回一律用 {@link UserVO}，不含 password。</p>
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 分页查询：current(默认1) size(默认10) username(模糊) */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(value = "current", required = false) Integer current,
                                           @RequestParam(value = "size", required = false) Integer size,
                                           @RequestParam(value = "username", required = false) String username,
                                           HttpServletRequest request) {
        CurrentUserUtil.requireAdmin(request);
        Page<UserVO> page = userService.page(
                current == null ? 1L : current.longValue(),
                size == null ? 10L : size.longValue(),
                username);
        return Result.success(toPageData(page));
    }

    /** 新增账号；username 唯一、password 至少 6 位 */
    @PostMapping
    public Result<UserVO> create(@RequestBody UserDTO dto, HttpServletRequest request) {
        CurrentUserUtil.requireAdmin(request);
        return Result.success("新增成功", userService.create(dto));
    }

    /** 修改账号；password 为空则不修改密码 */
    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable("id") Integer id,
                                 @RequestBody UserDTO dto,
                                 HttpServletRequest request) {
        CurrentUserUtil.requireAdmin(request);
        return Result.success("修改成功", userService.update(id, dto, CurrentUserUtil.getUserId(request)));
    }

    /** 启用/禁用；status 只允许 0 或 1，不能禁用自己 */
    @PutMapping("/{id}/status")
    public Result<UserVO> updateStatus(@PathVariable("id") Integer id,
                                       @RequestBody UserDTO dto,
                                       HttpServletRequest request) {
        CurrentUserUtil.requireAdmin(request);
        Integer status = dto == null ? null : dto.getStatus();
        return Result.success("操作成功", userService.updateStatus(id, status, CurrentUserUtil.getUserId(request)));
    }

    /** 删除账号；不能删除自己，发布过日程的用户也不能删 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Integer id, HttpServletRequest request) {
        CurrentUserUtil.requireAdmin(request);
        userService.delete(id, CurrentUserUtil.getUserId(request));
        return Result.success("删除成功", null);
    }

    /**
     * 契约要求分页 data 固定为 {records,total,current,size}，
     * 这里显式构造，避免把 MyBatis-Plus Page 的内部字段一起返回给前端。
     */
    private static Map<String, Object> toPageData(Page<UserVO> page) {
        Map<String, Object> data = new LinkedHashMap<>(4);
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return data;
    }
}
