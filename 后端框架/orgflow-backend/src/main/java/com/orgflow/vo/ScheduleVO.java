package com.orgflow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日程信息（对外返回）。
 *
 * <p>时间字段用 {@link LocalDateTime}，由全局 JacksonConfig 统一序列化成
 * {@code yyyy-MM-dd HH:mm:ss}，保证与契约一致。</p>
 */
@Data
public class ScheduleVO {

    private Integer id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 类型：1 公司公告 2 个人/部门日程 */
    private Integer type;

    /** 开始时间，格式 yyyy-MM-dd HH:mm:ss */
    private LocalDateTime startTime;

    /** 结束时间，格式 yyyy-MM-dd HH:mm:ss */
    private LocalDateTime endTime;

    /** 发布人（user 表 id，不是 employee id） */
    private Integer createBy;

    /** 发布人姓名（user → employee），无关联员工时回退为登录账号 */
    private String createByName;

    /** 创建时间，格式 yyyy-MM-dd HH:mm:ss */
    private LocalDateTime createTime;

    /** 当前登录人是否有权修改/删除本条日程（前端控制按钮显隐用） */
    private Boolean canEdit;
}
