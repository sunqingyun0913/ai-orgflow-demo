package com.orgflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.orgflow.common.BusinessException;
import com.orgflow.dto.DepartmentDTO;
import com.orgflow.entity.Department;
import com.orgflow.entity.Employee;
import com.orgflow.mapper.DepartmentMapper;
import com.orgflow.mapper.EmployeeMapper;
import com.orgflow.service.DepartmentService;
import com.orgflow.vo.DepartmentTreeVO;
import com.orgflow.vo.DepartmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 部门服务实现：列表（带人数统计）、树、增删改。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    /** 部门名称最大长度，对应 department.name varchar(50) */
    private static final int NAME_MAX_LENGTH = 50;
    /** 顶级部门的 parent_id 约定值 */
    private static final int ROOT_PARENT_ID = 0;
    private static final int DEFAULT_SORT_ORDER = 0;

    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;

    @Override
    public List<DepartmentVO> listAll() {
        List<Department> departments = listOrdered();
        Map<Integer, Long> employeeCounts = countEmployeesByDepartment();
        return departments.stream()
                .map(department -> toVO(department, employeeCounts.getOrDefault(department.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public List<DepartmentTreeVO> tree() {
        // 先查全量再在内存里组树：部门数量少，一次查询远好过递归查库
        List<Department> departments = listOrdered();
        Map<Integer, DepartmentTreeVO> nodes = new LinkedHashMap<>();
        for (Department department : departments) {
            DepartmentTreeVO node = new DepartmentTreeVO();
            node.setId(department.getId());
            node.setName(department.getName());
            node.setParentId(department.getParentId());
            node.setSortOrder(department.getSortOrder());
            nodes.put(department.getId(), node);
        }

        List<DepartmentTreeVO> roots = new ArrayList<>();
        for (DepartmentTreeVO node : nodes.values()) {
            Integer parentId = node.getParentId();
            DepartmentTreeVO parent = (parentId == null || parentId == ROOT_PARENT_ID) ? null : nodes.get(parentId);
            if (parent == null) {
                // 父节点不存在（脏数据）时当根节点处理，避免整棵子树在前端消失
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    @Override
    public DepartmentVO getById(Integer id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw BusinessException.notFound("部门不存在");
        }
        Long employeeCount = employeeMapper.selectCount(
                Wrappers.<Employee>lambdaQuery().eq(Employee::getDepartmentId, id));
        return toVO(department, employeeCount == null ? 0L : employeeCount);
    }

    @Override
    public DepartmentVO create(DepartmentDTO dto) {
        String name = validateName(dto);
        Integer parentId = normalizeParentId(dto.getParentId());
        requireParentExists(parentId);

        Department department = new Department();
        department.setName(name);
        department.setParentId(parentId);
        department.setSortOrder(dto.getSortOrder() == null ? DEFAULT_SORT_ORDER : dto.getSortOrder());
        departmentMapper.insert(department);
        log.info("新增部门：id={} name={} parentId={}", department.getId(), name, parentId);
        // create_time/update_time 由数据库默认值生成，重新查一次返回完整数据
        return getById(department.getId());
    }

    @Override
    public DepartmentVO update(Integer id, DepartmentDTO dto) {
        Department exists = departmentMapper.selectById(id);
        if (exists == null) {
            throw BusinessException.notFound("部门不存在");
        }
        String name = validateName(dto);
        Integer parentId = normalizeParentId(dto.getParentId());
        if (parentId.equals(id)) {
            throw BusinessException.badRequest("上级部门不能是自己");
        }
        if (parentId != ROOT_PARENT_ID) {
            requireParentExists(parentId);
            // 防成环：上级部门落在自己的子孙里会让树形成闭环，前端递归渲染会死循环
            if (collectDescendantIds(id).contains(parentId)) {
                throw BusinessException.badRequest("上级部门不能是自己的下级部门");
            }
        }
        // sortOrder 没传时保持原值，避免前端漏传把排序清成 0
        Integer sortOrder = dto.getSortOrder() == null ? exists.getSortOrder() : dto.getSortOrder();

        Department update = new Department();
        update.setId(id);
        update.setName(name);
        update.setParentId(parentId);
        update.setSortOrder(sortOrder);
        departmentMapper.updateById(update);
        log.info("修改部门：id={} name={} parentId={}", id, name, parentId);
        return getById(id);
    }

    @Override
    public void delete(Integer id) {
        if (departmentMapper.selectById(id) == null) {
            throw BusinessException.notFound("部门不存在");
        }
        // 提示语按契约固定，先判员工再判子部门
        Long employeeCount = employeeMapper.selectCount(
                Wrappers.<Employee>lambdaQuery().eq(Employee::getDepartmentId, id));
        if (employeeCount != null && employeeCount > 0) {
            throw BusinessException.badRequest("该部门下还有员工，无法删除");
        }
        Long childCount = departmentMapper.selectCount(
                Wrappers.<Department>lambdaQuery().eq(Department::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw BusinessException.badRequest("该部门下还有子部门，无法删除");
        }
        departmentMapper.deleteById(id);
        log.info("删除部门：id={}", id);
    }

    /** 按 sort_order、id 升序取全量部门 */
    private List<Department> listOrdered() {
        return departmentMapper.selectList(Wrappers.<Department>lambdaQuery()
                .orderByAsc(Department::getSortOrder)
                .orderByAsc(Department::getId));
    }

    /** 一次查出全部员工再按 departmentId 分组计数，避免按部门逐个 count（N+1） */
    private Map<Integer, Long> countEmployeesByDepartment() {
        List<Employee> employees = employeeMapper.selectList(
                Wrappers.<Employee>lambdaQuery().select(Employee::getDepartmentId));
        return employees.stream()
                .map(Employee::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /** 广度优先收集某部门的全部子孙ID，用于修改时的防环校验 */
    private Set<Integer> collectDescendantIds(Integer rootId) {
        List<Department> all = departmentMapper.selectList(Wrappers.<Department>lambdaQuery()
                .select(Department::getId, Department::getParentId));
        Map<Integer, List<Integer>> childrenByParent = new HashMap<>();
        for (Department department : all) {
            Integer parentId = department.getParentId() == null ? ROOT_PARENT_ID : department.getParentId();
            childrenByParent.computeIfAbsent(parentId, key -> new ArrayList<>()).add(department.getId());
        }

        Set<Integer> descendants = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>(childrenByParent.getOrDefault(rootId, List.of()));
        while (!queue.isEmpty()) {
            Integer current = queue.poll();
            // 已访问过就不再入队：既去重，也在历史脏数据成环时保证不会死循环
            if (!descendants.add(current)) {
                continue;
            }
            queue.addAll(childrenByParent.getOrDefault(current, List.of()));
        }
        return descendants;
    }

    private String validateName(DepartmentDTO dto) {
        if (dto == null) {
            throw BusinessException.badRequest("请求参数不能为空");
        }
        String name = dto.getName() == null ? null : dto.getName().trim();
        if (name == null || name.isEmpty()) {
            throw BusinessException.badRequest("部门名称不能为空");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw BusinessException.badRequest("部门名称长度不能超过 50 个字");
        }
        return name;
    }

    /** parentId 为 null 时按顶级部门（0）处理 */
    private Integer normalizeParentId(Integer parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }

    private void requireParentExists(Integer parentId) {
        if (parentId == ROOT_PARENT_ID) {
            return;
        }
        if (departmentMapper.selectById(parentId) == null) {
            throw BusinessException.badRequest("上级部门不存在");
        }
    }

    private DepartmentVO toVO(Department department, long employeeCount) {
        DepartmentVO vo = new DepartmentVO();
        vo.setId(department.getId());
        vo.setName(department.getName());
        vo.setParentId(department.getParentId());
        vo.setSortOrder(department.getSortOrder());
        vo.setCreateTime(department.getCreateTime());
        vo.setUpdateTime(department.getUpdateTime());
        vo.setEmployeeCount((int) employeeCount);
        return vo;
    }
}
