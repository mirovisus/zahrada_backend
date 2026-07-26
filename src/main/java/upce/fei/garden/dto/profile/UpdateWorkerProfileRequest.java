package upce.fei.garden.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import upce.fei.garden.validation.rules.ValidCzechPhone;

/**
 * Aktualizace profilu zahradníka. Telefonní číslo, bio a heslo jsou volitelné;
 * prázdný řetězec u {@code newPassword} znamená, že heslo zůstává beze změny.
 */
@Schema(description = "Aktualizace profilu zahradníka")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkerProfileRequest {
    @NotBlank(message = "Jméno je povinné")
    @Size(max = 50, message = "Jméno nesmí překročit 50 znaků")
    private String firstName;

    @NotBlank(message = "Příjmení je povinné")
    @Size(max = 50, message = "Příjmení nesmí překročit 50 znaků")
    private String lastName;

    @Schema(description = "Krátký text o zahradníkovi zobrazovaný vlastníkům zahrad")
    @Size(max = 1000, message = "Bio nesmí překročit 1000 znaků")
    private String bio;

    @NotBlank(message = "E-mail je povinný")
    @Email(message = "Neplatný formát e-mailu")
    private String email;

    @Schema(description = "Nepovinné, formát +420 XXX XXX XXX (mezery volitelné)")
    @ValidCzechPhone
    private String phoneNumber;

    @Schema(description = "Nové heslo; prázdný řetězec ponechá stávající heslo beze změny")
    // Prázdný řetězec = heslo se nemění, proto nelze použít prosté @Size(min = 8) (to by prázdnou hodnotu odmítlo).
    @Pattern(regexp = "^$|.{8,}$", message = "Heslo musí mít alespoň 8 znaků")
    private String newPassword;

    private String avatarUrl; // TODO: implementovat nahrávání souborů
}
