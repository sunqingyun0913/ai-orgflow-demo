package com.orgflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orgflow.common.BusinessException;
import com.orgflow.dto.ScheduleDTO;
import com.orgflow.entity.Employee;
import com.orgflow.entity.Schedule;
import com.orgflow.entity.User;
import com.orgflow.mapper.EmployeeMapper;
import com.orgflow.mapper.ScheduleMapper;
import com.orgflow.mapper.UserMapper;
import com.orgflow.service.ScheduleService;
import com.orgflow.vo.ScheduleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 日程管理实现（契约 4.4）。
 *
 * <p>权限规则：管理员可增删改任何日程；普通员工只能增删改自己发布的日程
 * （{@code create_by} = 自己的 user 表 id），并且不能发布或改成公司公告（type=1）。</p>
 *
 * <p>{@code create_by} 存的是 user 表 id，展示姓名时走 user → employee。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    /** 公司公告 */
    private static final int TYPE_ANNOUNCEMENT = 1;
    /** 个人/部门日程 */
    private static final int TYPE_PERSONAL = 2;
    /** 标题最大长度 */
    private static final int MAX_TITLE_LENGTH = 100;
    /** 单页最大条数，与 MybatisPlusConfig 里的 maxLimit 保持一致 */
    private static final long MAX_PAGE_SIZE = 500L;

    private final ScheduleMapper scheduleMapper;
    private final UserMapper userMapper;
    private final EmployeeMapper employeeMapper;

    @Override
    public Page<ScheduleVO> page(long current, long size, String title, Integer type, Integer currentUserId, boolean admin) {
        Page<Schedule> pageParam = new Page<>(normalizeCurrent(current), normalizeSize(size));
        LambdaQueryWrapper<Schedule> wrapper = Wrappers.<Schedule>lambdaQuery();
        if (StringUtils.hasText(title)) {
            wrapper.like(Schedule::getTitle, title.trim());
        }
        if (type != null) {
            wrapper.eq(Schedule::getType, type);
        }
        // MySQL 在 DESC 排序时把 NULL 排在最后，正好满足「start_time 为空排最后」；
        // 再按 id 倒序，保证分页结果稳定不跳行。
        wrapper.orderByDesc(Schedule::getStartTime).orderByDesc(Schedule::getId);

        Page<Schedule> result = scheduleMapper.selectPage(pageParam, wrapper);
        return toVoPage(result, currentUserId, admin);
    }

    @Override
    public List<ScheduleVO> announcements(Integer currentUserId, boolean admin) {
        List<Schedule> list = scheduleMapper.selectList(Wrappers.<Schedule>lambdaQuery()
                .eq(Schedule::getType, TYPE_ANNOUNCEMENT)
                .orderByDesc(Schedule::getStartTime)
                .orderByDesc(Schedule::getId));
        return toVoList(list, currentUserId, admin);
    }

    @Override
    public ScheduleVO detail(Integer id, Integer currentUserId, boolean admin) {
        Schedule schedule = getExisting(id);
        return toVO(schedule, currentUserId, admin, resolveCreateNames(List.of(schedule)));
    }

    @Override
    public ScheduleVO create(ScheduleDTO dto, Integer currentUserId, boolean admin) {
        ScheduleForm form = validate(dto);
        if (form.type() == TYPE_ANNOUNCEMENT && !admin) {
            throw BusinessException.forbidden("只有管理员可以发布公司公告");
        }

        Schedule schedule = new Schedule();
        schedule.setTitle(form.title());
        schedule.setContent(form.content());
        schedule.setType(form.type());
        schedule.setStartTime(form.startTime());
        schedule.setEndTime(form.endTime());
        // 发布人一律取 token 里的 user id，请求体里传了也不采纳
        schedule.setCreateBy(currentUserId);
        LocalDateTime now = LocalDateTime.now();
        schedule.setCreateTime(now);
        schedule.setUpdateTime(now);
        scheduleMapper.insert(schedule);
        log.info("用户 {} 新增日程 {}（type={}）", currentUserId, schedule.getId(), schedule.getType());
        return detail(schedule.getId(), currentUserId, admin);
    }

    @Override
    public ScheduleVO update(Integer id, ScheduleDTO dto, Integer currentUserId, boolean admin) {
        Schedule schedule = getExisting(id);
        checkModifyPermission(schedule, currentUserId, admin, "只能修改自己发布的日程");

        ScheduleForm form = validate(dto);
        // 普通员工既不能发布公告，也不能把已有日程改成公告
        if (form.type() == TYPE_ANNOUNCEMENT && !admin) {
            throw BusinessException.forbidden("只有管理员可以发布公司公告");
        }

        // 用 UpdateWrapper 显式 set，空的时间才能被真正清成 NULL；
        // create_by / create_time 不允许被改写。
        LambdaUpdateWrapper<Schedule> update = Wrappers.<Schedule>lambdaUpdate()
                .eq(Schedule::getId, id)
                .set(Schedule::getTitle, form.title())
                .set(Schedule::getContent, form.content())
                .set(Schedule::getType, form.type())
                .set(Schedule::getStartTime, form.startTime())
                .set(Schedule::getEndTime, form.endTime())
                .set(Schedule::getUpdateTime, LocalDateTime.now());
        scheduleMapper.update(null, update);
        log.info("用户 {} 修改日程 {}", currentUserId, id);
        return detail(id, currentUserId, admin);
    }

    @Override
    public void delete(Integer id, Integer currentUserId, boolean admin) {
        Schedule schedule = getExisting(id);
        checkModifyPermission(schedule, currentUserId, admin, "只能删除自己发布的日程");
        scheduleMapper.deleteById(id);
        log.info("用户 {} 删除日程 {}", currentUserId, id);
    }

    /** 取日程，不存在抛 404「日程不存在」 */
    private Schedule getExisting(Integer id) {
        if (id == null) {
            throw BusinessException.badRequest("日程 id 不能为空");
        }
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw BusinessException.notFound("日程不存在");
        }
        return schedule;
    }

    /** 管理员放行；普通员工只能操作自己发布的日程，否则 403 */
    private void checkModifyPermission(Schedule schedule, Integer currentUserId, boolean admin, String message) {
        if (admin) {
            return;
        }
        if (currentUserId == null || !Objects.equals(schedule.getCreateBy(), currentUserId)) {
            throw BusinessException.forbidden(message);
        }
    }

    /** 入参校验，返回校验通过的表单 */
    private ScheduleForm validate(ScheduleDTO dto) {
        if (dto == null) {
            throw BusinessException.badRequest("请求参数不能为空");
        }
        String title = dto.getTitle() == null ? null : dto.getTitle().trim();
        if (!StringUtils.hasText(title)) {
            throw BusinessException.badRequest("标题不能为空");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw BusinessException.badRequest("标题长度不能超过 " + MAX_TITLE_LENGTH + " 字");
        }

        int type = dto.getType() == null ? TYPE_PERSONAL : dto.getType();
        if (type != TYPE_ANNOUNCEMENT && type != TYPE_PERSONAL) {
            throw BusinessException.badRequest("日程类型只能是 1（公司公告）或 2（个人/部门日程）");
        }

        LocalDateTime startTime = dto.getStartTime();
        LocalDateTime endTime = dto.getEndTime();
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw BusinessException.badRequest("开始时间不能晚于结束时间");
        }
        return new ScheduleForm(title, dto.getContent(), type, startTime, endTime);
    }

    /** 校验通过的日程表单（createBy 不在其中，一律取 token） */
    private record ScheduleForm(String title, String content, int type, LocalDateTime startTime, LocalDateTime endTime) {
    }

    private Page<ScheduleVO> toVoPage(Page<Schedule> page, Integer currentUserId, boolean admin) {
        Page<ScheduleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(toVoList(page.getRecords(), currentUserId, admin));
        return voPage;
    }

    private List<ScheduleVO> toVoList(List<Schedule> list, Integer currentUserId, boolean admin) {
        List<ScheduleVO> voList = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return voList;
        }
        Map<Integer, String> nameMap = resolveCreateNames(list);
        for (Schedule schedule : list) {
            voList.add(toVO(schedule, currentUserId, admin, nameMap));
        }
        return voList;
    }

    private ScheduleVO toVO(Schedule schedule, Integer currentUserId, boolean admin, Map<Integer, String> nameMap) {
        ScheduleVO vo = new ScheduleVO();
        vo.setId(schedule.getId());
        vo.setTitle(schedule.getTitle());
        vo.setContent(schedule.getContent());
        vo.setType(schedule.getType());
        vo.setStartTime(schedule.getStartTime());
        vo.setEndTime(schedule.getEndTime());
        vo.setCreateBy(schedule.getCreateBy());
        vo.setCreateByName(nameMap.get(schedule.getCreateBy()));
        vo.setCreateTime(schedule.getCreateTime());
        // 管理员可以改任何日程；普通员工只有自己发布的才能改（前端据此控制按钮显隐）
        vo.setCanEdit(admin || (currentUserId != null && Objects.equals(schedule.getCreateBy(), currentUserId)));
        return vo;
    }

    /**
     * 批量把 create_by（user 表 id）解析成姓名：user → employee.name。
     * 没有关联员工时回退成登录账号，避免前端显示空白。
     */
    private Map<Integer, String> resolveCreateNames(List<Schedule> list) {
        Map<Integer, String> nameMap = new HashMap<>();
        Set<Integer> userIds = new LinkedHashSet<>();
        for (Schedule schedule : list) {
            if (schedule != null && schedule.getCreateBy() != null) {
                userIds.add(schedule.getCreateBy());
            }
        }
        if (userIds.isEmpty()) {
            return nameMap;
        }

        Map<Integer, User> userMap = new HashMap<>();
        Set<Integer> employeeIds = new LinkedHashSet<>();
        for (User user : userMapper.selectByIds(userIds)) {
            userMap.put(user.getId(), user);
            if (user.getEmployeeId() != null) {
                employeeIds.add(user.getEmployeeId());
            }
        }

        Map<Integer, String> employeeNames = new HashMap<>();
        if (!employeeIds.isEmpty()) {
            for (Employee employee : employeeMapper.selectByIds(employeeIds)) {
                employeeNames.put(employee.getId(), employee.getName());
            }
        }

        for (Integer userId : userIds) {
            User user = userMap.get(userId);
            if (user == null) {
                continue;
            }
            String employeeName = user.getEmployeeId() == null ? null : employeeNames.get(user.getEmployeeId());
            nameMap.put(userId, StringUtils.hasText(employeeName) ? employeeName : user.getUsername());
        }
        return nameMap;
    }

    /** current 小于 1 时按第 1 页处理，避免 offset 为负导致 SQL 报错 */
    private static long normalizeCurrent(long current) {
        return current < 1 ? 1 : current;
    }

    /** size 非法时按 10 处理，并限制单页最大条数 */
    private static long normalizeSize(long size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
