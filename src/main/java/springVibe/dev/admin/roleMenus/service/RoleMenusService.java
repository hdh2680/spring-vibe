package springVibe.dev.admin.roleMenus.service;

import springVibe.dev.admin.roleMenus.domain.Menu;
import springVibe.dev.admin.roleMenus.domain.Role;
import springVibe.dev.admin.roleMenus.dto.RoleMenusForm;
import springVibe.dev.admin.roleMenus.mapper.RoleMenusMapper;
import springVibe.system.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoleMenusService {
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final RoleMenusMapper roleMenusMapper;

    public RoleMenusService(RoleMenusMapper roleMenusMapper) {
        this.roleMenusMapper = roleMenusMapper;
    }

    public List<Role> findAllRoles() {
        return roleMenusMapper.findAllRoles();
    }

    public Role findRoleById(Long id) {
        return roleMenusMapper.findRoleById(id);
    }

    public List<Long> findGrantedMenuIds(Long roleId) {
        return roleMenusMapper.findMenuIdsByRoleId(roleId);
    }

    public List<Menu> findMenuTree() {
        List<Menu> flat = roleMenusMapper.findAllEnabledMenus();
        if (flat == null || flat.isEmpty()) {
            return List.of();
        }

        Map<Long, Menu> byId = new HashMap<>();
        for (Menu m : flat) {
            if (m != null && m.getId() != null) {
                byId.put(m.getId(), m);
            }
        }

        for (Menu m : byId.values()) {
            Long pid = m.getParentId();
            if (pid != null) {
                Menu parent = byId.get(pid);
                if (parent != null) {
                    parent.getChildren().add(m);
                }
            }
        }

        Comparator<Menu> bySort = Comparator
            .comparing((Menu m) -> m.getSortOrder() == null ? 0 : m.getSortOrder())
            .thenComparing(m -> m.getId() == null ? 0L : m.getId());

        List<Menu> roots = new ArrayList<>();
        for (Menu m : byId.values()) {
            if (m.getParentId() == null || !byId.containsKey(m.getParentId())) {
                roots.add(m);
            }
        }

        roots.sort(bySort);
        for (Menu r : roots) {
            r.getChildren().sort(bySort);
        }
        return roots;
    }

    @Transactional
    public void save(RoleMenusForm form) {
        Role role = roleMenusMapper.findRoleById(form.getRoleId());
        if (role == null) {
            throw new BaseException("ROLE_NOT_FOUND", "권한(role)을 찾을 수 없습니다.");
        }
        if (role.getRoleName() != null && ROLE_ADMIN.equalsIgnoreCase(role.getRoleName().trim())) {
            // Guardrail: avoid locking yourself out of admin pages.
            throw new BaseException("ROLE_ADMIN_IMMUTABLE", "ROLE_ADMIN의 메뉴 권한은 이 화면에서 변경할 수 없습니다.");
        }

        roleMenusMapper.deleteRoleMenus(form.getRoleId());
        if (form.getMenuIds() == null) {
            return;
        }
        for (Long menuId : form.getMenuIds()) {
            if (menuId == null) {
                continue;
            }
            roleMenusMapper.insertRoleMenu(form.getRoleId(), menuId);
        }
    }
}

