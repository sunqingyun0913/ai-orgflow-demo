package com.orgflow.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 员工信息（对外返回，不包含任何敏感字段）
 */
@Data
public class EmployeeVO {

    private Integer id;
    private String name;
    private Integer departmentId;
    /** 所属部门名称，连表查询得到 */
    private String departmentName;
    private String phone;
    private String email;
    private String position;
    private LocalDate entryDate;
    /** 状态：1在职 0离职 */
    private Integer status;
}
