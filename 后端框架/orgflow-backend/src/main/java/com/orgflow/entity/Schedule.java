package com.orgflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("schedule")
public class Schedule {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String content;
    private Integer type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}