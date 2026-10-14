package com.orgflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orgflow.common.Result;
import com.orgflow.dto.ScheduleDTO;
import com.orgflow.service.ScheduleService;
import com.orgflow.util.CurrentUserUtil;
import com.orgflow.vo.ScheduleVO;
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
import java.util.List;
import java.util.Map;

/**
 * 日程接口（契约 4.4）。
 *
 * <p>当前用户由 {@link CurrentUserUtil} 从 token 解析；管理员与普通员工的
 * 权限差异在 Service 里判断。创建人 createBy 一律取 token，前端传了也忽略。</p>
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /** 分页查询：current(默认1) size(默认10) title(模糊) type */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(value = "current", required = false) Integer current,
                                           @RequestParam(value = "size", required = false) Integer size,
                                           @RequestParam(value = "title", required = false) String title,
                                           @RequestParam(value = "type", required = false) Integer type,
                                           HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        boolean admin = CurrentUserUtil.isAdmin(request);
        Page<ScheduleVO> page = scheduleService.page(
                current == null ? 1L : current.longValue(),
                size == null ? 10L : size.longValue(),
                title, type, userId, admin);
        return Result.success(toPageData(page));
    }

    /** 全部公司公告（type=1，按 start_time 倒序） */
    @GetMapping("/announcements")
    public Result<List<ScheduleVO>> announcements(HttpServletRequest request) {
        return Result.success(scheduleService.announcements(
                CurrentUserUtil.getUserId(request), CurrentUserUtil.isAdmin(request)));
    }

    /** 详情（带 createByName） */
    @GetMapping("/{id}")
    public Result<ScheduleVO> detail(@PathVariable("id") Integer id, HttpServletRequest request) {
        return Result.success(scheduleService.detail(
                id, CurrentUserUtil.getUserId(request), CurrentUserUtil.isAdmin(request)));
    }

    /** 新增；type 默认 2，公司公告(type=1) 仅管理员可发布 */
    @PostMapping
    public Result<ScheduleVO> create(@RequestBody ScheduleDTO dto, HttpServletRequest request) {
        return Result.success("新增成功", scheduleService.create(
                dto, CurrentUserUtil.getUserId(request), CurrentUserUtil.isAdmin(request)));
    }

    /** 修改；普通员工只能改自己发布的日程 */
    @PutMapping("/{id}")
    public Result<ScheduleVO> update(@PathVariable("id") Integer id,
                                     @RequestBody ScheduleDTO dto,
                                     HttpServletRequest request) {
        return Result.success("修改成功", scheduleService.update(
                id, dto, CurrentUserUtil.getUserId(request), CurrentUserUtil.isAdmin(request)));
    }

    /** 删除；普通员工只能删自己发布的日程 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Integer id, HttpServletRequest request) {
        scheduleService.delete(id, CurrentUserUtil.getUserId(request), CurrentUserUtil.isAdmin(request));
        return Result.success("删除成功", null);
    }

    /**
     * 契约要求分页 data 固定为 {records,total,current,size}，
     * 这里显式构造，避免把 MyBatis-Plus Page 的内部字段一起返回给前端。
     */
    private static Map<String, Object> toPageData(Page<ScheduleVO> page) {
        Map<String, Object> data = new LinkedHashMap<>(4);
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return data;
    }
}
