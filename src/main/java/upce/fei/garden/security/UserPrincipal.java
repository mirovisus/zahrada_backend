package upce.fei.garden.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import upce.fei.garden.model.User;

import java.util.Collection;
import java.util.List;

/**
 * Adaptér mezi doménovou entitou {@link User} a rozhraním Spring Security {@link UserDetails}.
 * Umožňuje po autentizaci získat zpět celou doménovou entitu (viz {@link CurrentUserService}).
 */
public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }
}
