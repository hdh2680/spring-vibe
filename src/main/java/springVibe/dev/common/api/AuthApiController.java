package springVibe.dev.common.api;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    public record MeResponse(
        boolean authenticated,
        String username,
        List<String> authorities
    ) {}

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        boolean authenticated = authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);

        if (!authenticated) {
            return new MeResponse(false, null, List.of());
        }

        List<String> authorities = authentication.getAuthorities() == null
            ? List.of()
            : authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        return new MeResponse(true, authentication.getName(), authorities);
    }
}

