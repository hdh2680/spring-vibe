package basemarkdown.dev.admin.users.mapper;

import basemarkdown.dev.admin.users.domain.AdminUser;
import basemarkdown.dev.admin.users.domain.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UsersMapper {
    List<AdminUser> findAll();

    AdminUser findById(@Param("id") Long id);

    AdminUser findByUsername(@Param("username") String username);

    int insert(AdminUser user);

    int update(AdminUser user);

    List<Role> findAllRoles();

    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    int deleteUserRoles(@Param("userId") Long userId);

    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}

