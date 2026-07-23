package upce.fei.garden.dto.garden;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Žádost o vytvoření nové zahrady – název, plocha a adresa.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateGardenRequest {
    @NotBlank(message = "Název zahrady je povinný")
    @Size(max = 100, message = "Název nesmí překročit 100 znaků")
    private String gardenName;

    @NotNull(message = "Plocha zahrady je povinná")
    @Positive(message = "Plocha musí být kladné číslo")
    private Double areaSqm;

    @NotBlank(message = "Město je povinné")
    @Size(min=2, max=50, message = "Nazev musí mít alespoň 2 znaky, maximalně 50 znaků")
    private String city;

    @NotBlank(message = "Ulice je povinná")
    @Size(min=1, max=50, message = "Ulice musí mít alespoň 1 znak, maximalně 50 znaků")
    private String street;

    @NotBlank(message = "Číslo domu je povinné")
    @Size(min=1, max=5, message = "Číslo popisné musí obsahovat 1-5 znaků")
    private String houseNumber;

    @NotBlank(message = "PSČ je povinné")
    @Pattern(regexp = "^\\d{3}\\s?\\d{2}$", message = "PSČ musí mít formát 5 číslic, volitelně oddělených mezerou (např. 53003 nebo 530 03)")
    private String postalCode;

    // TODO: Double lawnAreaSqm
    // TODO: Boolean hasPool
    // TODO: Boolean hasTrees
}
