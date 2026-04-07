package springVibe.system.web;

import springVibe.dev.common.domain.Menu;
import springVibe.dev.common.service.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class MenuModelAdvice {
    private final MenuService menuService;
    private final String frontendDevOrigin;

    public MenuModelAdvice(
        MenuService menuService,
        @Value("${app.frontend.dev-origin:}") String frontendDevOrigin
    ) {
        this.menuService = menuService;
        this.frontendDevOrigin = frontendDevOrigin;
    }

    @ModelAttribute("frontendDevOrigin")
    public String frontendDevOrigin() {
        return frontendDevOrigin;
    }

    @ModelAttribute("leftMenus")
    public List<Menu> leftMenus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return List.of();
        }
        boolean isAdmin = authentication.getAuthorities() != null
            && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return menuService.findAllEnabledLeftMenus();
        }
        return menuService.findLeftMenusByUsername(authentication.getName());
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        return authentication.getAuthorities() != null
            && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request == null ? null : request.getRequestURI();
    }
}
