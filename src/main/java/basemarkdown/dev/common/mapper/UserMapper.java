package basemarkdown.dev.common.mapper;

import basemarkdown.dev.common.domain.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    UserAccount findByUsername(@Param("username") String username);

    List<String> findRoleNamesByUserId(@Param("userId") Long userId);
}

