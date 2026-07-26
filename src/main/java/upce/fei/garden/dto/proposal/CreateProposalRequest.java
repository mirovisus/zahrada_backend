package upce.fei.garden.dto.proposal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Vytvoření návrhu zahradníkem – cena, popis a přiložený dokument.
 */
@Schema(description = "Návrh zahradníka na realizaci poptávky - lze podat jen na poptávku ve stavu NOVA, "
        + "jeden zahradník smí na poptávku podat nejvýš jeden návrh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProposalRequest {
    @Schema(description = "Nabízená cena v Kč")
    @NotNull(message = "Cena je povinná")
    @Positive(message = "Cena musí být kladné číslo")
    private BigDecimal price;

    @Size(max = 1000, message = "Popis nesmí překročit 1000 znaků")
    private String description;

    @Schema(description = "Zatím neimplementováno - nahrávání souborů není podporováno")
    // TODO: implementovat nahrávání PDF
    private String documentUrl;
}
