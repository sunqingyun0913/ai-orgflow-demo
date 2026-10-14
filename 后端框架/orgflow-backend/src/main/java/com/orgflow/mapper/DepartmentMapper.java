package com.orgflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orgflow.entity.Department;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}