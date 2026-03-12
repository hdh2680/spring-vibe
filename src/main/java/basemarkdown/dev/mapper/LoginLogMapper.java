package basemarkdown.dev.mapper;

import basemarkdown.dev.domain.LoginLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper {
    int insert(LoginLog log);
}
