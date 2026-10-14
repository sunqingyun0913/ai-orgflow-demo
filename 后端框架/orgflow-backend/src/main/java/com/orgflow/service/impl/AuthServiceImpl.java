package com.orgflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.orgflow.common.BusinessException;
import com.orgflow.dto.LoginDTO;
import com.orgflow.entity.Department;
import com.orgflow.entity.Employee;
import com.orgflow.entity.User;
import com.orgflow.mapper.DepartmentMapper;
import com.orgflow.mapper.EmployeeMapper;
import com.orgflow.mapper.UserMapper;
import com.orgflow.service.AuthService;
import com.orgflow.util.JwtUtil;
import com.orgflow.util.PasswordUtil;
import com.orgflow.vo.EmployeeVO;
import com.orgflow.vo.LoginVO;
import com.orgflow.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现：账号密码登录 + JWT 签发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        if (loginDTO == null || isBlank(loginDTO.getUsername()) || isBlank(loginDTO.getPassword())) {
            throw BusinessException.badRequest("账号和密码不能为空");
        }
        String username = loginDTO.getUsername().trim();

        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) {
            throw BusinessException.badRequest("账号或密码错误");
        }
        if (!PasswordUtil.matches(loginDTO.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("账号或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw BusinessException.forbidden("该账号已被禁用，请联系管理员");
        }

        String token = jwtUtil.createToken(user.getId(), user.getEmployeeId(), user.getRole(), user.getUsername());

        UserVO userVO = toUserVO(user);
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(userVO);
        loginVO.setDisplayName(userVO.getEmployee() != null ? userVO.getEmployee().getName() : user.getUsername());
        log.info("用户登录成功：{}（角色 {}）", username, user.getRole());
        return loginVO;
    }

    @Override
    public UserVO getCurrentUserInfo(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.unauthorized("用户不存在，请重新登录");
        }
        return toUserVO(user);
    }

    @Override
    public void updatePassword(Integer userId, String oldPassword, String newPassword) {
        if (isBlank(oldPassword) || isBlank(newPassword)) {
            throw BusinessException.badRequest("原密码和新密码都不能为空");
        }
        if (newPassword.length() < 6) {
            throw BusinessException.badRequest("新密码长度不能少于 6 位");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.unauthorized("用户不存在，请重新登录");
        }
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw BusinessException.badRequest("原密码不正确");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(update);
        log.info("用户 {} 修改了密码", user.getUsername());
    }

    /** 组装用户信息（含关联员工与部门） */
    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmployeeId(user.getEmployeeId());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setEmployee(buildEmployeeVO(user.getEmployeeId()));
        return vo;
    }

    private EmployeeVO buildEmployeeVO(Integer employeeId) {
        if (employeeId == null) {
            return null;
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            return null;
        }
        EmployeeVO vo = new EmployeeVO();
        vo.setId(employee.getId());
        vo.setName(employee.getName());
        vo.setDepartmentId(employee.getDepartmentId());
        vo.setPhone(employee.getPhone());
        vo.setEmail(employee.getEmail());
        vo.setPosition(employee.getPosition());
        vo.setEntryDate(employee.getEntryDate());
        vo.setStatus(employee.getStatus());
        if (employee.getDepartmentId() != null) {
            Department department = departmentMapper.selectById(employee.getDepartmentId());
            if (department != null) {
                vo.setDepartmentName(department.getName());
            }
        }
        return vo;
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
