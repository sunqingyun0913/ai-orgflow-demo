package com.orgflow.service;

import com.orgflow.dto.EmployeeDTO;
import com.orgflow.vo.EmployeeOptionVO;
import com.orgflow.vo.EmployeeVO;

import java.util.List;
import java.util.Map;

/**
 * 员工（通讯录）服务。
 */
public interface EmployeeService {

    /**
     * 分页查询员工。
     *
     * @param current      页码，默认 1
     * @param size         每页条数，默认 10，最大 100
     * @param name         姓名模糊查询
     * @param departmentId 部门筛选
     * @param status       状态筛选：1在职 0离职
     * @param assignee     传 "1" 时进入「日程指派」模式：强制 size=500、只看在职员工、按 id 升序
     * @return 契约约定的分页结构 {records, total, current, size}
     */
    Map<String, Object> pageEmployees(Integer current, Integer size, String name,
                                      Integer departmentId, Integer status, String assignee);

    /** 在职员工下拉选项（按 id 升序） */
    List<EmployeeOptionVO> listOptions();

    /** 员工详情（带所属部门名称） */
    EmployeeVO getById(Integer id);

    /** 新增员工，返回新增后的完整数据 */
    EmployeeVO create(EmployeeDTO dto);

    /** 修改员工，返回修改后的完整数据 */
    EmployeeVO update(Integer id, EmployeeDTO dto);

    /** 删除员工 */
    void delete(Integer id);
}
