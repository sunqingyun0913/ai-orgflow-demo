package com.orgflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orgflow.common.BusinessException;
import com.orgflow.dto.EmployeeDTO;
import com.orgflow.entity.Department;
import com.orgflow.entity.Employee;
import com.orgflow.entity.User;
import com.orgflow.mapper.DepartmentMapper;
import com.orgflow.mapper.EmployeeMapper;
import com.orgflow.mapper.UserMapper;
import com.orgflow.service.EmployeeService;
import com.orgflow.vo.EmployeeOptionVO;
import com.orgflow.vo.EmployeeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 员工（通讯录）服务实现：分页查询、下拉选项、增删改。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    /** 姓名最大长度，对应 employee.name varchar(20) */
    private static final int NAME_MAX_LENGTH = 20;
    /** 邮箱最大长度，对应 employee.email varchar(50) */
    private static final int EMAIL_MAX_LENGTH = 50;
    /** 职位最大长度，对应 employee.position varchar(20) */
    private static final int POSITION_MAX_LENGTH = 20;
    /** 日程指派模式下强制返回的条数（与分页插件 maxLimit=500 对齐） */
    private static final int ASSIGNEE_PAGE_SIZE = 500;
    /** 普通分页允许的最大 size，避免前端一次把全表拉走 */
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final long DEFAULT_CURRENT = 1L;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_INACTIVE = 0;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    @Override
    public Map<String, Object> pageEmployees(Integer current, Integer size, String name,
                                             Integer departmentId, Integer status, String assignee) {
        boolean assigneeMode = "1".equals(assignee);
        // 指派模式是给日程「指派人员」下拉用的：固定第一页 500 条、只看在职、按 id 升序
        long pageNo = assigneeMode ? DEFAULT_CURRENT : normalizeCurrent(current);
        long pageSize = assigneeMode ? ASSIGNEE_PAGE_SIZE : normalizeSize(size);

        LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery();
        if (assigneeMode) {
            wrapper.eq(Employee::getStatus, STATUS_ACTIVE);
        } else {
            wrapper.eq(departmentId != null, Employee::getDepartmentId, departmentId)
                    .eq(status != null, Employee::getStatus, status);
        }
        String keyword = name == null ? null : name.trim();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Employee::getName, keyword);
        }
        wrapper.orderByAsc(Employee::getId);

        // 显式声明 Page 变量，避免依赖泛型推断
        Page<Employee> pageRequest = new Page<>(pageNo, pageSize);
        Page<Employee> pageResult = employeeMapper.selectPage(pageRequest, wrapper);

        Map<String, Object> data = new LinkedHashMap<>(4);
        data.put("records", toVOList(pageResult.getRecords()));
        data.put("total", pageResult.getTotal());
        data.put("current", pageNo);
        data.put("size", pageSize);
        return data;
    }

    @Override
    public List<EmployeeOptionVO> listOptions() {
        List<Employee> employees = employeeMapper.selectList(Wrappers.<Employee>lambdaQuery()
                .eq(Employee::getStatus, STATUS_ACTIVE)
                .orderByAsc(Employee::getId));
        if (employees.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, String> departmentNames = loadDepartmentNames(employees);
        return employees.stream().map(employee -> {
            EmployeeOptionVO vo = new EmployeeOptionVO();
            vo.setId(employee.getId());
            vo.setName(employee.getName());
            if (employee.getDepartmentId() != null) {
                vo.setDepartmentName(departmentNames.get(employee.getDepartmentId()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public EmployeeVO getById(Integer id) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw BusinessException.notFound("员工不存在");
        }
        return toVO(employee, loadDepartmentNames(List.of(employee)));
    }

    @Override
    public EmployeeVO create(EmployeeDTO dto) {
        NormalizedEmployee value = normalize(dto);
        Employee employee = new Employee();
        employee.setName(value.name());
        employee.setDepartmentId(value.departmentId());
        employee.setPhone(value.phone());
        employee.setEmail(value.email());
        employee.setPosition(value.position());
        employee.setEntryDate(value.entryDate());
        employee.setStatus(value.status());
        employeeMapper.insert(employee);
        log.info("新增员工：id={} name={} departmentId={}", employee.getId(), value.name(), value.departmentId());
        // 数据库负责生成 create_time/update_time，重新查一次才能把完整数据返回前端
        return getById(employee.getId());
    }

    @Override
    public EmployeeVO update(Integer id, EmployeeDTO dto) {
        if (employeeMapper.selectById(id) == null) {
            throw BusinessException.notFound("员工不存在");
        }
        NormalizedEmployee value = normalize(dto);
        // 用 UpdateWrapper 显式 set 每个字段：updateById 会忽略 null 字段，
        // 那样「清空手机号 / 邮箱」这类操作就保存不了
        LambdaUpdateWrapper<Employee> update = Wrappers.<Employee>lambdaUpdate()
                .eq(Employee::getId, id)
                .set(Employee::getName, value.name())
                .set(Employee::getDepartmentId, value.departmentId())
                .set(Employee::getPhone, value.phone())
                .set(Employee::getEmail, value.email())
                .set(Employee::getPosition, value.position())
                .set(Employee::getEntryDate, value.entryDate())
                .set(Employee::getStatus, value.status());
        employeeMapper.update(null, update);
        log.info("修改员工：id={} name={}", id, value.name());
        return getById(id);
    }

    @Override
    public void delete(Integer id) {
        if (employeeMapper.selectById(id) == null) {
            throw BusinessException.notFound("员工不存在");
        }
        // 被 user 表关联的员工删掉会让账号变成孤儿数据，直接拦下
        Long relatedUsers = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getEmployeeId, id));
        if (relatedUsers != null && relatedUsers > 0) {
            throw BusinessException.badRequest("该员工已关联系统账号，无法删除");
        }
        employeeMapper.deleteById(id);
        log.info("删除员工：id={}", id);
    }

    /** current 默认 1，小于 1 一律按 1 处理 */
    private long normalizeCurrent(Integer current) {
        if (current == null || current < 1) {
            return DEFAULT_CURRENT;
        }
        return current;
    }

    /** size 默认 10，并限制在 1~100 */
    private long normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * 校验并整理入参。
     *
     * @return 校验后的字段值，供新增/修改复用，避免两处重复写校验逻辑
     */
    private NormalizedEmployee normalize(EmployeeDTO dto) {
        if (dto == null) {
            throw BusinessException.badRequest("请求参数不能为空");
        }
        String name = trimToNull(dto.getName());
        if (name == null) {
            throw BusinessException.badRequest("员工姓名不能为空");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw BusinessException.badRequest("员工姓名长度不能超过 20 个字");
        }

        // 0 视为「未分配部门」：department 主键从 1 开始，0 不是合法部门
        Integer departmentId = (dto.getDepartmentId() == null || dto.getDepartmentId() == 0)
                ? null : dto.getDepartmentId();
        if (departmentId != null && departmentMapper.selectById(departmentId) == null) {
            throw BusinessException.badRequest("所属部门不存在");
        }

        String phone = trimToNull(dto.getPhone());
        if (phone != null && !phone.matches("\\d{11}")) {
            throw BusinessException.badRequest("手机号必须为 11 位数字");
        }
        String email = trimToNull(dto.getEmail());
        if (email != null && !email.contains("@")) {
            throw BusinessException.badRequest("邮箱格式不正确，必须包含 @");
        }
        if (email != null && email.length() > EMAIL_MAX_LENGTH) {
            throw BusinessException.badRequest("邮箱长度不能超过 50 个字符");
        }
        String position = trimToNull(dto.getPosition());
        if (position != null && position.length() > POSITION_MAX_LENGTH) {
            throw BusinessException.badRequest("职位长度不能超过 20 个字");
        }

        Integer status = dto.getStatus() == null ? STATUS_ACTIVE : dto.getStatus();
        if (status != STATUS_ACTIVE && status != STATUS_INACTIVE) {
            throw BusinessException.badRequest("状态值不正确，只能是 1（在职）或 0（离职）");
        }
        return new NormalizedEmployee(name, departmentId, phone, email, position,
                parseEntryDate(dto.getEntryDate()), status);
    }

    /** 前端日期控件清空时会传空串，这里按 null 处理；格式错误给 400 而不是 500 */
    private LocalDate parseEntryDate(String text) {
        String value = trimToNull(text);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw BusinessException.badRequest("入职日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    /** 一次性把涉及的部门查出来做成 id→name 映射，避免每条员工都查一次部门（N+1） */
    private Map<Integer, String> loadDepartmentNames(List<Employee> employees) {
        Set<Integer> departmentIds = employees.stream()
                .map(Employee::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return departmentMapper.selectList(Wrappers.<Department>lambdaQuery()
                        .select(Department::getId, Department::getName)
                        .in(Department::getId, departmentIds))
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (first, second) -> first));
    }

    private List<EmployeeVO> toVOList(List<Employee> employees) {
        if (employees == null || employees.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, String> departmentNames = loadDepartmentNames(employees);
        return employees.stream().map(employee -> toVO(employee, departmentNames)).collect(Collectors.toList());
    }

    private EmployeeVO toVO(Employee employee, Map<Integer, String> departmentNames) {
        EmployeeVO vo = new EmployeeVO();
        vo.setId(employee.getId());
        vo.setName(employee.getName());
        vo.setDepartmentId(employee.getDepartmentId());
        if (employee.getDepartmentId() != null) {
            vo.setDepartmentName(departmentNames.get(employee.getDepartmentId()));
        }
        vo.setPhone(employee.getPhone());
        vo.setEmail(employee.getEmail());
        vo.setPosition(employee.getPosition());
        vo.setEntryDate(employee.getEntryDate());
        vo.setStatus(employee.getStatus());
        return vo;
    }

    private static String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 校验后的员工字段（create/update 共用） */
    private record NormalizedEmployee(String name, Integer departmentId, String phone, String email,
                                      String position, LocalDate entryDate, Integer status) {
    }
}
