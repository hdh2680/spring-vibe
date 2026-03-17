package springVibe.system.security;

import springVibe.dev.common.domain.Menu;
import springVibe.dev.common.service.MenuService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.List;

/**
 * Redirect to the first accessible menu after login (user_roles -> role_menus -> menus).
 */
public class MenuRedirectSuccessHandler implements AuthenticationSuccessHandler {
    private final MenuService menuService;

    public MenuRedirectSuccessHandler(MenuService menuService) {
        this.menuService = menuService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException, ServletException {
        String username = authentication == null ? null : authentication.getName();
        String target = firstMenuPath(username);
        response.sendRedirect(toAppRelativeUrl(request, target));
    }

    private String firstMenuPath(String username) {
        List<Menu> roots = menuService.findLeftMenusByUsername(username);
        String path = firstPathPreferChildren(roots);
        return (path == null || path.isBlank()) ? "/" : path;
    }

    private static String toAppRelativeUrl(HttpServletRequest request, String path) {
        if (path == null || path.isBlank()) {
            path = "/";
        }
        String ctx = request.getContextPath();
        if (ctx == null) {
            ctx = "";
        }
        if (!ctx.isEmpty() && path.startsWith("/") && !path.startsWith(ctx + "/")) {
            return ctx + path;
        }
        return path;
    }

    // Prefer children when a menu is a grouping node (e.g., /admin with children).
    private static String firstPathPreferChildren(List<Menu> menus) {
        if (menus == null) {
            return null;
        }
        for (Menu m : menus) {
            if (m == null) {
                continue;
            }
            String childPath = firstPathPreferChildren(m.getChildren());
            if (childPath != null && !childPath.isBlank()) {
                return childPath;
            }
            if (m.getPath() != null && !m.getPath().isBlank()) {
                return m.getPath();
            }
        }
        return null;
    }
}
