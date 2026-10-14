package com.orgflow.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点。
 *
 * <p>children 初始化为空集合而不是 null：契约要求树节点始终带 children 数组，
 * 前端 el-table 的树形表格也依赖它判断「有没有子节点」。</p>
 */
@Data
public class DepartmentTreeVO {

    private Integer id;
    private String name;
    private Integer parentId;
    private Integer sortOrder;

    /** 子部门，无子部门时为空数组 */
    private List<DepartmentTreeVO> children = new ArrayList<>();
}
