package upce.fei.garden.dto.auth;


import jakarta.validation.constraints.*;
import lombok.*;
import upce.fei.garden.model.enums.UserRole;

/**
 * Registrace nového uživatele – zákazník i zahradník na jedné stránce.
 * Role se přepíná výběrem na frontendu (OWNER / WORKER).
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Jméno je povinné")
    @Size(max = 100, message = "Jméno nesmí překročit 100 znaků")
    private String firstName;

    @NotBlank(message = "Příjmení je povinné")
    @Size(max = 100, message = "Příjmení nesmí překročit 100 znaků")
    private String lastName;

    @NotNull(message = "Role je povinná")
    private UserRole role;

    @NotBlank(message = "E-mail je povinný")
    @Email(message = "Neplatný formát e-mailu")
    @Size(max = 100, message = "E-mail nesmí překročit 100 znaků")
    private String email;

    @NotBlank(message = "Heslo je povinné")
    @Size(min = 8, max = 100, message = "Heslo musí mít alespoň 8 znaků")
    private String password;
}
