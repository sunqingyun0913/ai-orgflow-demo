package com.orgflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("employee")
public class Employee {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private Integer departmentId;
    private String phone;
    private String email;
    private String position;
    private LocalDate entryDate;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}