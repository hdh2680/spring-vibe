package springVibe.dev.admin.roleMenus.mapper;

import springVibe.dev.admin.roleMenus.domain.Menu;
import springVibe.dev.admin.roleMenus.domain.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMenusMapper {
    List<Role> findAllRoles();

    Role findRoleById(@Param("id") Long id);

    List<Menu> findAllEnabledMenus();

    List<Long> findMenuIdsByRoleId(@Param("roleId") Long roleId);

    int deleteRoleMenus(@Param("roleId") Long roleId);

    int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
}

