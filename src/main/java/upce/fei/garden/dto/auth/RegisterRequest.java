package upce.fei.garden.dto.auth;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import upce.fei.garden.model.enums.UserRole;

/**
 * Registrace nového uživatele – zákazník i zahradník na jedné stránce.
 * Role se přepíná výběrem na frontendu (OWNER / WORKER).
 */
@Schema(description = "Žádost o registraci nového uživatele - vlastníka zahrady nebo zahradníka")
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

    @Schema(description = "Role určuje, zda se uživatel bude chovat jako vlastník zahrady, nebo jako zahradník")
    @NotNull(message = "Role je povinná")
    private UserRole role;

    @NotBlank(message = "E-mail je povinný")
    @Email(message = "Neplatný formát e-mailu")
    @Size(max = 100, message = "E-mail nesmí překročit 100 znaků")
    private String email;

    @Schema(description = "Heslo v čitelné podobě - na serveru se ukládá pouze BCrypt hash")
    @NotBlank(message = "Heslo je povinné")
    @Size(min = 8, max = 100, message = "Heslo musí mít alespoň 8 znaků")
    private String password;
}
