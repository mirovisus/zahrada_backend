package upce.fei.garden.dto.auth;

import upce.fei.garden.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Odpověď po úspěšné registraci nebo přihlášení. Obsahuje JWT token pro autorizaci.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long id;
    private String email;
    private UserRole role;
    private String token;
}
