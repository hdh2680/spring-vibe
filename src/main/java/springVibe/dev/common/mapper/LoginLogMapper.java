package springVibe.dev.common.mapper;

import springVibe.dev.common.domain.LoginLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper {
    int insert(LoginLog log);
}

