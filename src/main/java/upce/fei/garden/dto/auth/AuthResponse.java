package upce.fei.garden.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import upce.fei.garden.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Odpověď po úspěšné registraci nebo přihlášení. Obsahuje JWT token pro autorizaci.
 */
@Schema(description = "Odpověď po úspěšné registraci nebo přihlášení")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long id;
    private String email;
    private UserRole role;

    @Schema(description = "JWT token - posílá se v hlavičce Authorization: Bearer <token>")
    private String token;
}
