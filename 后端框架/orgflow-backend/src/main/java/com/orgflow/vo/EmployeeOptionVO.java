package com.orgflow.vo;

import lombok.Data;

/**
 * 员工下拉选项（日程指派、筛选部门负责人等场景用），只暴露必要字段。
 */
@Data
public class EmployeeOptionVO {

    private Integer id;
    private String name;

    /** 所属部门名称，便于前端下拉里直接显示「张三（技术部）」 */
    private String departmentName;
}
