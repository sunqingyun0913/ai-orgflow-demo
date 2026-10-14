package com.orgflow.dto;

import lombok.Data;

/**
 * 部门新增/修改入参。
 *
 * <p>单独定义 DTO 而不是直接接收实体，是为了让前端无法传入 id、createTime 等
 * 不应由前端决定的字段，也便于后续单独调整校验规则。</p>
 */
@Data
public class DepartmentDTO {

    /** 部门名称，必填且不超过 50 字 */
    private String name;

    /** 上级部门ID；为 null 时按顶级部门（0）处理 */
    private Integer parentId;

    /** 显示排序；为 null 时按 0 处理（修改时不传则保持原值） */
    private Integer sortOrder;
}
