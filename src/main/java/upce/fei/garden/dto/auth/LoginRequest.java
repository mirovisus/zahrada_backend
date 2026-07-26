package upce.fei.garden.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Přihlášení uživatele žadá e-mail a heslo. Role se určuje automaticky podle účtu.
 */
@Schema(description = "Přihlašovací údaje - role se dohledá automaticky podle účtu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Email je povinný")
    @Email(message = "Neplatná emailová adresa")
    private String email;

    @NotBlank(message = "Heslo je povinné")
    private String password;
}
