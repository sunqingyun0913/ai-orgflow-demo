package com.orgflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orgflow.common.BusinessException;
import com.orgflow.dto.UserDTO;
import com.orgflow.entity.Department;
import com.orgflow.entity.Employee;
import com.orgflow.entity.Schedule;
import com.orgflow.entity.User;
import com.orgflow.mapper.DepartmentMapper;
import com.orgflow.mapper.EmployeeMapper;
import com.orgflow.mapper.ScheduleMapper;
import com.orgflow.mapper.UserMapper;
import com.orgflow.service.UserService;
import com.orgflow.util.PasswordUtil;
import com.orgflow.vo.EmployeeVO;
import com.orgflow.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 用户账号管理实现（契约 4.5）。
 *
 * <p>整块仅管理员可用，权限校验在 Controller 里做（requireAdmin）；
 * 这里只做业务校验。所有返回值都是 {@link UserVO}，绝不携带 password。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int ROLE_ADMIN = 1;
    private static final int ROLE_EMPLOYEE = 2;
    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;
    /** 密码最小长度 */
    private static final int MIN_PASSWORD_LENGTH = 6;
    /** 单页最大条数，与 MybatisPlusConfig 里的 maxLimit 保持一致 */
    private static final long MAX_PAGE_SIZE = 500L;

    private final UserMapper userMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final ScheduleMapper scheduleMapper;

    @Override
    public Page<UserVO> page(long current, long size, String username) {
        Page<User> pageParam = new Page<>(normalizeCurrent(current), normalizeSize(size));
        LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery();
        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username.trim());
        }
        wrapper.orderByAsc(User::getId);

        Page<User> result = userMapper.selectPage(pageParam, wrapper);
        Page<UserVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toVoList(result.getRecords()));
        return voPage;
    }

    @Override
    public UserVO create(UserDTO dto) {
        if (dto == null) {
            throw BusinessException.badRequest("请求参数不能为空");
        }
        String username = trimToNull(dto.getUsername());
        if (username == null) {
            throw BusinessException.badRequest("登录账号不能为空");
        }
        if (existsUsername(username, null)) {
            throw BusinessException.badRequest("登录账号已存在");
        }
        String password = dto.getPassword();
        if (!StringUtils.hasText(password)) {
            throw BusinessException.badRequest("密码不能为空");
        }
        checkPasswordLength(password);

        Integer role = normalizeRole(dto.getRole());
        Integer status = normalizeStatus(dto.getStatus());
        Integer employeeId = checkEmployee(dto.getEmployeeId());

        User user = new User();
        user.setUsername(username);
        // 数据库只存 BCrypt 密文
        user.setPassword(PasswordUtil.encode(password));
        user.setEmployeeId(employeeId);
        user.setRole(role);
        user.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        userMapper.insert(user);
        log.info("新增用户账号 {}（id={}，角色 {}）", username, user.getId(), role);
        return toVO(user);
    }

    @Override
    public UserVO update(Integer id, UserDTO dto, Integer currentUserId) {
        if (id == null) {
            throw BusinessException.badRequest("用户 id 不能为空");
        }
        if (dto == null) {
            throw BusinessException.badRequest("请求参数不能为空");
        }
        User exist = getExisting(id);

        // 契约里修改接口不带 username：只有前端明确传了且与当前不同，才当成改账号并校验唯一
        String username = trimToNull(dto.getUsername());
        boolean changeUsername = username != null && !username.equals(exist.getUsername());
        if (changeUsername && existsUsername(username, id)) {
            throw BusinessException.badRequest("登录账号已存在");
        }

        Integer role = dto.getRole() == null ? null : normalizeRole(dto.getRole());
        Integer status = dto.getStatus() == null ? null : normalizeStatus(dto.getStatus());
        Integer employeeId = checkEmployee(dto.getEmployeeId());
        if (status != null && status == STATUS_DISABLED && Objects.equals(id, currentUserId)) {
            // 防止管理员把自己禁用后再也登不进来
            throw BusinessException.badRequest("不能禁用自己的账号");
        }
        if (role != null && role != ROLE_ADMIN && Objects.equals(id, currentUserId)) {
            // 防止管理员把自己降权后系统里再也没有管理员（页面上没有自救入口）
            throw BusinessException.badRequest("不能降低自己的角色，请让其他管理员操作");
        }

        LambdaUpdateWrapper<User> update = Wrappers.<User>lambdaUpdate().eq(User::getId, id);
        if (changeUsername) {
            update.set(User::getUsername, username);
        }
        // password 为空表示不修改密码
        if (StringUtils.hasText(dto.getPassword())) {
            checkPasswordLength(dto.getPassword());
            update.set(User::getPassword, PasswordUtil.encode(dto.getPassword()));
        }
        // employeeId 传了才改，不传（null）保持原来的关联
        if (employeeId != null) {
            update.set(User::getEmployeeId, employeeId);
        }
        if (role != null) {
            update.set(User::getRole, role);
        }
        if (status != null) {
            update.set(User::getStatus, status);
        }
        update.set(User::getUpdateTime, LocalDateTime.now());
        userMapper.update(null, update);
        log.info("修改用户账号 {}（id={}）", exist.getUsername(), id);
        return toVO(getExisting(id));
    }

    @Override
    public UserVO updateStatus(Integer id, Integer status, Integer currentUserId) {
        if (id == null) {
            throw BusinessException.badRequest("用户 id 不能为空");
        }
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw BusinessException.badRequest("状态只能是 0（禁用）或 1（启用）");
        }
        User exist = getExisting(id);
        if (status == STATUS_DISABLED && Objects.equals(id, currentUserId)) {
            throw BusinessException.badRequest("不能禁用自己的账号");
        }

        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, id)
                .set(User::getStatus, status)
                .set(User::getUpdateTime, LocalDateTime.now()));
        exist.setStatus(status);
        log.info("用户账号 {} 状态改为 {}", exist.getUsername(), status);
        return toVO(exist);
    }

    @Override
    public void delete(Integer id, Integer currentUserId) {
        if (id == null) {
            throw BusinessException.badRequest("用户 id 不能为空");
        }
        User exist = getExisting(id);
        if (Objects.equals(id, currentUserId)) {
            throw BusinessException.badRequest("不能删除自己的账号");
        }
        // 该用户发布过日程就不允许删除，否则日程的 create_by 会变成悬空引用
        Long scheduleCount = scheduleMapper.selectCount(Wrappers.<Schedule>lambdaQuery().eq(Schedule::getCreateBy, id));
        if (scheduleCount != null && scheduleCount > 0) {
            throw BusinessException.badRequest("该用户已发布日程，无法删除");
        }
        userMapper.deleteById(id);
        log.info("删除用户账号 {}", exist.getUsername());
    }

    /** 取用户，不存在抛 404「用户不存在」 */
    private User getExisting(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    /** 登录账号是否已存在（excludeId 用于修改时排除自己） */
    private boolean existsUsername(String username, Integer excludeId) {
        LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery().eq(User::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(User::getId, excludeId);
        }
        Long count = userMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    /** 关联员工必须真实存在，不传返回 null */
    private Integer checkEmployee(Integer employeeId) {
        if (employeeId == null) {
            return null;
        }
        if (employeeMapper.selectById(employeeId) == null) {
            throw BusinessException.badRequest("关联员工不存在");
        }
        return employeeId;
    }

    /** 新增用：不传默认普通员工 */
    private static Integer normalizeRole(Integer role) {
        if (role == null) {
            return ROLE_EMPLOYEE;
        }
        if (role != ROLE_ADMIN && role != ROLE_EMPLOYEE) {
            throw BusinessException.badRequest("角色只能是 1（管理员）或 2（普通员工）");
        }
        return role;
    }

    /** 新增用：不传默认启用 */
    private static Integer normalizeStatus(Integer status) {
        if (status == null) {
            return STATUS_ENABLED;
        }
        if (status != STATUS_ENABLED && status != STATUS_DISABLED) {
            throw BusinessException.badRequest("状态只能是 0（禁用）或 1（启用）");
        }
        return status;
    }

    private static void checkPasswordLength(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw BusinessException.badRequest("密码长度不能少于 " + MIN_PASSWORD_LENGTH + " 位");
        }
    }

    private static String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** current 小于 1 时按第 1 页处理 */
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

    private List<UserVO> toVoList(List<User> users) {
        List<UserVO> voList = new ArrayList<>();
        if (users == null || users.isEmpty()) {
            return voList;
        }

        // 批量取员工与部门，避免 N+1 查询
        Set<Integer> employeeIds = new LinkedHashSet<>();
        for (User user : users) {
            if (user.getEmployeeId() != null) {
                employeeIds.add(user.getEmployeeId());
            }
        }
        Map<Integer, Employee> employeeMap = new HashMap<>();
        Map<Integer, String> departmentNames = new HashMap<>();
        if (!employeeIds.isEmpty()) {
            Set<Integer> departmentIds = new LinkedHashSet<>();
            for (Employee employee : employeeMapper.selectByIds(employeeIds)) {
                employeeMap.put(employee.getId(), employee);
                if (employee.getDepartmentId() != null) {
                    departmentIds.add(employee.getDepartmentId());
                }
            }
            if (!departmentIds.isEmpty()) {
                for (Department department : departmentMapper.selectByIds(departmentIds)) {
                    departmentNames.put(department.getId(), department.getName());
                }
            }
        }

        for (User user : users) {
            voList.add(toVO(user, employeeMap, departmentNames));
        }
        return voList;
    }

    /** 单个用户的 VO（新增/修改后用） */
    private UserVO toVO(User user) {
        return toVoList(Collections.singletonList(user)).get(0);
    }

    /** 组装 UserVO：绝不拷贝 password；没有关联员工时 employee 为 null */
    private UserVO toVO(User user, Map<Integer, Employee> employeeMap, Map<Integer, String> departmentNames) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmployeeId(user.getEmployeeId());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());

        Employee employee = user.getEmployeeId() == null ? null : employeeMap.get(user.getEmployeeId());
        if (employee != null) {
            EmployeeVO employeeVO = new EmployeeVO();
            employeeVO.setId(employee.getId());
            employeeVO.setName(employee.getName());
            employeeVO.setDepartmentId(employee.getDepartmentId());
            employeeVO.setDepartmentName(departmentNames.get(employee.getDepartmentId()));
            employeeVO.setPhone(employee.getPhone());
            employeeVO.setEmail(employee.getEmail());
            employeeVO.setPosition(employee.getPosition());
            employeeVO.setEntryDate(employee.getEntryDate());
            employeeVO.setStatus(employee.getStatus());
            vo.setEmployee(employeeVO);
        }
        return vo;
    }
}
