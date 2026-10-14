package com.orgflow.controller;

import com.orgflow.common.Result;
import com.orgflow.dto.EmployeeDTO;
import com.orgflow.service.EmployeeService;
import com.orgflow.util.CurrentUserUtil;
import com.orgflow.vo.EmployeeOptionVO;
import com.orgflow.vo.EmployeeVO;
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

import java.util.List;
import java.util.Map;

/**
 * 员工通讯录接口 /api/employee。
 *
 * <p>查询类接口登录后即可访问；增删改通过 {@link CurrentUserUtil#requireAdmin} 限制为管理员。
 * 参数名一律显式写在注解里，不依赖编译期 -parameters，避免换构建方式后绑定失败。</p>
 */
@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * 分页查询；assignee=1 时是日程「指派人员」下拉专用：
     * 强制 size=500、只返回在职员工，返回结构仍然是 {records,total,current,size}。
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(value = "current", required = false) Integer current,
                                            @RequestParam(value = "size", required = false) Integer size,
                                            @RequestParam(value = "name", required = false) String name,
                                            @RequestParam(value = "departmentId", required = false) Integer departmentId,
                                            @RequestParam(value = "status", required = false) Integer status,
                                            @RequestParam(value = "assignee", required = false) String assignee) {
        return Result.success(employeeService.pageEmployees(current, size, name, departmentId, status, assignee));
    }

    /** 在职员工下拉选项（id + name + departmentName） */
    @GetMapping("/options")
    public Result<List<EmployeeOptionVO>> options() {
        return Result.success(employeeService.listOptions());
    }

    /** 员工详情 */
    @GetMapping("/{id}")
    public Result<EmployeeVO> detail(@PathVariable("id") Integer id) {
        return Result.success(employeeService.getById(id));
    }

    /** 新增员工（仅管理员） */
    @PostMapping
    public Result<EmployeeVO> create(@RequestBody EmployeeDTO employeeDTO, HttpServletRequest request) {
        CurrentUserUtil.requireAdmin(request);
        return Result.success("新增成功", employeeService.create(employeeDTO));
    }

    /** 修改员工（仅管理员） */
    @PutMapping("/{id}")
    public Result<EmployeeVO> update(@PathVariable("id") Integer id, @RequestBody EmployeeDTO employeeDTO,
                                     HttpServletRequest request) {
        CurrentUserUtil.requireAdmin(request);
        return Result.success("修改成功", employeeService.update(id, employeeDTO));
    }

    /** 删除员工（仅管理员） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Integer id, HttpServletRequest request) {
        CurrentUserUtil.requireAdmin(request);
        employeeService.delete(id);
        return Result.success("删除成功", null);
    }
}
