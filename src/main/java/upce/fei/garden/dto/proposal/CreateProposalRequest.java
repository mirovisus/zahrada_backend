package upce.fei.garden.dto.proposal;

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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProposalRequest {
    @NotNull(message = "Cena je povinná")
    @Positive(message = "Cena musí být kladné číslo")
    private BigDecimal price;

    @Size(max = 1000, message = "Popis nesmí překročit 1000 znaků")
    private String description;

    // TODO: implementovat nahrávání PDF
    private String documentUrl;
}
