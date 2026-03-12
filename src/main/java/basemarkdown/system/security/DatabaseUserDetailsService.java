package basemarkdown.system.security;

import basemarkdown.dev.domain.UserAccount;
import basemarkdown.dev.service.AuthService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final AuthService authService;

    public DatabaseUserDetailsService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = authService.findByUsername(username);
        if (account == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        boolean enabled = account.getStatus() == null || "ACTIVE".equalsIgnoreCase(account.getStatus());

        List<String> roleNames = authService.findRoleNamesByUserId(account.getId());
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roleNames != null) {
            for (String roleName : roleNames) {
                if (roleName != null && !roleName.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(roleName.trim()));
                }
            }
        }
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return User.withUsername(account.getUsername())
            .password(account.getPassword())
            .authorities(authorities)
            .disabled(!enabled)
            .build();
    }
}
