package basemarkdown.system.web;

import basemarkdown.dev.common.domain.Menu;
import basemarkdown.dev.common.service.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class MenuModelAdvice {
    private final MenuService menuService;

    public MenuModelAdvice(MenuService menuService) {
        this.menuService = menuService;
    }

    @ModelAttribute("leftMenus")
    public List<Menu> leftMenus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return List.of();
        }
        return menuService.findLeftMenusByUsername(authentication.getName());
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request == null ? null : request.getRequestURI();
    }
}
