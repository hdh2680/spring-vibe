package basemarkdown.dev.common.mapper;

import basemarkdown.dev.common.domain.LoginLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper {
    int insert(LoginLog log);
}

