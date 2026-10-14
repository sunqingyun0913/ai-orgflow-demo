package com.orgflow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门信息（列表、详情、下拉展示通用）
 */
@Data
public class DepartmentVO {

    private Integer id;
    private String name;
    private Integer parentId;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 该部门下的员工人数（列表接口会填充，用于前端展示和删除校验提示） */
    private Integer employeeCount;
}
