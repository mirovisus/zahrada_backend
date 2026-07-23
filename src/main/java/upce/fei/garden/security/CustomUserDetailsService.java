package upce.fei.garden.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import upce.fei.garden.model.User;
import upce.fei.garden.repository.UserRepository;

/**
 * Načítá uživatele pro potřeby Spring Security podle e-mailu (ten slouží jako username).
 * Roli z {@link upce.fei.garden.model.enums.UserRole} mapuje na authority s prefixem {@code ROLE_}
 * (viz {@link UserPrincipal#getAuthorities()}).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Uživatel s e-mailem '" + email + "' nebyl nalezen."));
        return new UserPrincipal(user);
    }
}
