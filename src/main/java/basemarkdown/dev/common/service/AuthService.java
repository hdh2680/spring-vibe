package basemarkdown.dev.common.service;

import basemarkdown.dev.common.domain.LoginLog;
import basemarkdown.dev.common.domain.UserAccount;
import basemarkdown.dev.common.mapper.LoginLogMapper;
import basemarkdown.dev.common.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {
    public static final String LOGIN_RESULT_SUCCESS = "SUCCESS";
    public static final String LOGIN_RESULT_FAIL = "FAIL";

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;

    public AuthService(UserMapper userMapper, LoginLogMapper loginLogMapper) {
        this.userMapper = userMapper;
        this.loginLogMapper = loginLogMapper;
    }

    public UserAccount findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    public List<String> findRoleNamesByUserId(Long userId) {
        return userMapper.findRoleNamesByUserId(userId);
    }

    public void insertLoginLog(String username, String loginResult, String loginIp, String userAgent) {
        Long userId = null;
        try {
            UserAccount account = userMapper.findByUsername(username);
            if (account != null) {
                userId = account.getId();
            }
        } catch (Exception ignored) {
            // Auditing must not block authentication flow.
        }

        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setLoginIp(loginIp);
        log.setUserAgent(userAgent);
        log.setLoginResult(loginResult);

        try {
            loginLogMapper.insert(log);
        } catch (Exception ignored) {
            // Auditing must not block authentication flow.
        }
    }
}

