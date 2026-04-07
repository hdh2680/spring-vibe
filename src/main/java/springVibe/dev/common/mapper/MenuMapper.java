package springVibe.dev.common.mapper;

import springVibe.dev.common.domain.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuMapper {
    List<Menu> findAccessibleMenusByUsername(@Param("username") String username);

    List<Menu> findAllEnabledMenus();

    List<Menu> findEnabledMenusByIds(@Param("ids") List<Long> ids);
}

