package com.orgflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orgflow.dto.UserDTO;
import com.orgflow.vo.UserVO;

/**
 * 用户账号管理服务（契约 4.5，全部仅管理员可用，权限校验在 Controller）。
 *
 * <p>所有对外返回一律使用 {@link UserVO}，绝不返回 password。</p>
 */
public interface UserService {

    /** 分页查询：username 模糊 */
    Page<UserVO> page(long current, long size, String username);

    /** 新增账号，返回新增后的账号 */
    UserVO create(UserDTO dto);

    /** 修改账号（password 为空不改密码），返回修改后的账号 */
    UserVO update(Integer id, UserDTO dto, Integer currentUserId);

    /** 启用/禁用 */
    UserVO updateStatus(Integer id, Integer status, Integer currentUserId);

    /** 删除 */
    void delete(Integer id, Integer currentUserId);
}
