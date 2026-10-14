package com.orgflow.controller;

import com.orgflow.common.Result;
import com.orgflow.dto.DepartmentDTO;
import com.orgflow.service.DepartmentService;
import com.orgflow.util.CurrentUserUtil;
import com.orgflow.vo.DepartmentTreeVO;
import com.orgflow.vo.DepartmentVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门管理接口 /api/department。
 *
 * <p>查询类接口登录后即可访问；增删改通过 {@link CurrentUserUtil#requireAdmin} 限制为管理员。</p>
 */
@RestController
@RequestMapping("/api/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /** 全量列表（按 sort_order,id 升序），每条带员工人数 */
    @GetMapping("/list")
    public Result<List<DepartmentVO>> list() {
        return Result.success(departmentService.listAll());
    }

    /** 部门树 */
    @GetMapping("/tree")
    public Result<List<DepartmentTreeVO>> tree() {
        return Result.success(departmentService.tree());
    }

    /** 部门详情 */
    @GetMapping("/{id}")
    public Result<DepartmentVO> detail(@PathVariable("id") Integer id) {
        return Result.success(departmentService.getById(id));
    }

    /** 新增部门（仅管理员） */
    @PostMapping
    public Result<DepartmentVO> create(@RequestBody DepartmentDTO departmentDTO, HttpServletRequest request) {
        CurrentUserUtil.requireAdmin(request);
        return Result.success("新增成功", departmentService.create(departmentDTO));
    }

    /** 修改部门（仅管理员） */
    @PutMapping("/{id}")
    public Result<DepartmentVO> update(@PathVariable("id") Integer id, @RequestBody DepartmentDTO departmentDTO,
                                       HttpServletRequest request) {
        CurrentUserUtil.requireAdmin(request);
        return Result.success("修改成功", departmentService.update(id, departmentDTO));
    }

    /** 删除部门（仅管理员） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Integer id, HttpServletRequest request) {
        CurrentUserUtil.requireAdmin(request);
        departmentService.delete(id);
        return Result.success("删除成功", null);
    }
}
