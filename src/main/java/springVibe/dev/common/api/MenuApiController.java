package springVibe.dev.common.api;

import springVibe.dev.common.domain.Menu;
import springVibe.dev.common.service.MenuService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
public class MenuApiController {
    private final MenuService menuService;

    public MenuApiController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/left")
    public List<Menu> left(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return List.of();
        }

        boolean isAdmin = authentication.getAuthorities() != null
            && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (isAdmin) {
            return menuService.findAllEnabledLeftMenus();
        }
        return menuService.findLeftMenusByUsername(authentication.getName());
    }
}

