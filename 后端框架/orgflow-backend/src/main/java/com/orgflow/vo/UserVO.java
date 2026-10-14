package com.orgflow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号信息（对外返回，绝对不包含 password）
 */
@Data
public class UserVO {

    private Integer id;
    private String username;
    private Integer employeeId;
    /** 角色：1管理员 2普通员工 */
    private Integer role;
    /** 状态：1启用 0禁用 */
    private Integer status;
    private LocalDateTime createTime;

    /** 关联员工信息，可能为 null */
    private EmployeeVO employee;
}
