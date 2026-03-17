package springVibe.system.security;

import springVibe.dev.common.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class LoginAuditListener {
    private final AuthService authService;

    public LoginAuditListener(AuthService authService) {
        this.authService = authService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        insertLog(username, AuthService.LOGIN_RESULT_SUCCESS);
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        insertLog(username, AuthService.LOGIN_RESULT_FAIL);
    }

    private void insertLog(String username, String result) {
        HttpServletRequest request = currentRequest();

        String loginIp = null;
        String userAgent = null;
        if (request != null) {
            loginIp = extractClientIp(request);
            userAgent = request.getHeader("User-Agent");
        }

        authService.insertLoginLog(username, result, loginIp, userAgent);
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }

    private static String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Take the first IP in the list.
            int comma = xff.indexOf(',');
            return (comma >= 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
