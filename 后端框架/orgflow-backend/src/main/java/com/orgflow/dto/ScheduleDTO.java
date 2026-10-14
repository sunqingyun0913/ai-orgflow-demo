package com.orgflow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日程新增/修改入参。
 *
 * <p>时间字段用 {@link LocalDateTime}：全局 JacksonConfig 已把 java.time 的
 * 序列化/反序列化统一成 {@code yyyy-MM-dd HH:mm:ss}，
 * 前端直接传 {@code "2026-12-01 10:00:00"} 就能绑定。</p>
 *
 * <p>故意不包含 {@code createBy}：创建人一律从 token 取，前端传了也会被忽略。</p>
 */
@Data
public class ScheduleDTO {

    /** 标题，必填，不超过 100 字 */
    private String title;

    /** 日程内容 */
    private String content;

    /** 类型：1 公司公告 2 个人/部门日程；不传默认 2 */
    private Integer type;

    /** 开始时间，格式 yyyy-MM-dd HH:mm:ss */
    private LocalDateTime startTime;

    /** 结束时间，格式 yyyy-MM-dd HH:mm:ss */
    private LocalDateTime endTime;
}
