package basemarkdown.dev.admin.roleMenus.dto;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RoleMenusForm {
    @NotNull(message = "roleId는 필수입니다.")
    private Long roleId;

    private List<Long> menuIds = new ArrayList<>();

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public List<Long> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<Long> menuIds) {
        this.menuIds = menuIds;
    }
}

