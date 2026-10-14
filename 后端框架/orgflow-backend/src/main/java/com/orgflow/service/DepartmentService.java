package com.orgflow.service;

import com.orgflow.dto.DepartmentDTO;
import com.orgflow.vo.DepartmentTreeVO;
import com.orgflow.vo.DepartmentVO;

import java.util.List;

/**
 * 部门服务。
 */
public interface DepartmentService {

    /** 全量部门列表（按 sort_order,id 升序），每条带该部门的员工人数 */
    List<DepartmentVO> listAll();

    /** 部门树，parentId=0 为根，children 递归填充 */
    List<DepartmentTreeVO> tree();

    /** 部门详情 */
    DepartmentVO getById(Integer id);

    /** 新增部门，返回新增后的完整数据 */
    DepartmentVO create(DepartmentDTO dto);

    /** 修改部门，返回修改后的完整数据 */
    DepartmentVO update(Integer id, DepartmentDTO dto);

    /** 删除部门（有员工或有子部门时不允许） */
    void delete(Integer id);
}
