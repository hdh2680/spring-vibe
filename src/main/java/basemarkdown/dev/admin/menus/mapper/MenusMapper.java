package basemarkdown.dev.admin.menus.mapper;

import basemarkdown.dev.admin.menus.domain.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenusMapper {
    List<Menu> findAll();

    Menu findById(@Param("id") Long id);

    Menu findByMenuKey(@Param("menuKey") String menuKey);

    int insert(Menu menu);

    int update(Menu menu);

    int delete(@Param("id") Long id);

    List<Menu> findAllParents();
}
