package com.orgflow.dto;

import lombok.Data;

/**
 * 用户账号新增/修改入参。
 *
 * <p>新增：{@code username}、{@code password} 必填；
 * 修改：{@code password} 为空表示不修改密码。</p>
 */
@Data
public class UserDTO {

    /** 登录账号，新增必填 */
    private String username;

    /** 密码明文（后端用 BCrypt 加密后入库）；修改时为空表示不修改 */
    private String password;

    /** 关联员工 id，可为空；不为空时必须真实存在 */
    private Integer employeeId;

    /** 角色：1 管理员 2 普通员工；不传默认 2 */
    private Integer role;

    /** 状态：1 启用 0 禁用；不传默认 1 */
    private Integer status;
}
