package basemarkdown.dev.common.mapper;

import basemarkdown.dev.common.domain.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuMapper {
    List<Menu> findAccessibleMenusByUsername(@Param("username") String username);

    List<Menu> findEnabledMenusByIds(@Param("ids") List<Long> ids);
}

