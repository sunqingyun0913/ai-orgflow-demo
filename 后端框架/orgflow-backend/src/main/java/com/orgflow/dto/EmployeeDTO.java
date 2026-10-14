package com.orgflow.dto;

import lombok.Data;

/**
 * 员工新增/修改入参。
 *
 * <p>entryDate 故意用 String 而不是 LocalDate：前端日期控件被清空时会传空串 "",
 * Jackson 直接反序列化 LocalDate 会抛异常、被全局兜底变成 500；用 String 接收
 * 可以在这里统一转成 400 并给中文提示。格式仍按契约固定为 yyyy-MM-dd。</p>
 */
@Data
public class EmployeeDTO {

    /** 姓名，必填且不超过 20 字 */
    private String name;

    /** 所属部门ID；为空表示未分配部门 */
    private Integer departmentId;

    /** 手机号，选填；填了必须 11 位数字 */
    private String phone;

    /** 邮箱，选填；填了必须包含 @ */
    private String email;

    /** 职位 */
    private String position;

    /** 入职日期，选填，格式 yyyy-MM-dd */
    private String entryDate;

    /** 状态：1在职 0离职；为 null 时默认 1 */
    private Integer status;
}
