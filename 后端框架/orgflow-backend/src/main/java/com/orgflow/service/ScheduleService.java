package com.orgflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orgflow.dto.ScheduleDTO;
import com.orgflow.vo.ScheduleVO;

import java.util.List;

/**
 * 日程管理服务（契约 4.4）。
 *
 * <p>{@code currentUserId} / {@code admin} 由 Controller 从 token 解析后传入，
 * Service 只做业务与权限判断，不直接依赖 request。</p>
 */
public interface ScheduleService {

    /** 分页查询：title 模糊、type 精确；按 start_time 倒序，start_time 为空排最后 */
    Page<ScheduleVO> page(long current, long size, String title, Integer type, Integer currentUserId, boolean admin);

    /** 全部公司公告（type=1），按 start_time 倒序 */
    List<ScheduleVO> announcements(Integer currentUserId, boolean admin);

    /** 详情，带 createByName；不存在抛 404「日程不存在」 */
    ScheduleVO detail(Integer id, Integer currentUserId, boolean admin);

    /** 新增，返回新增后的日程 */
    ScheduleVO create(ScheduleDTO dto, Integer currentUserId, boolean admin);

    /** 修改，返回修改后的日程 */
    ScheduleVO update(Integer id, ScheduleDTO dto, Integer currentUserId, boolean admin);

    /** 删除 */
    void delete(Integer id, Integer currentUserId, boolean admin);
}
