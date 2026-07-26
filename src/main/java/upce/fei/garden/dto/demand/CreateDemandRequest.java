package upce.fei.garden.dto.demand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import upce.fei.garden.validation.rules.FutureOrToday;

import java.time.LocalDate;
import java.util.List;

/**
 * Vytvoření nové poptávky – popis prací a požadované datum realizace.
 */
@Schema(description = "Vytvoření nebo aktualizace poptávky vlastníkem zahrady")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateDemandRequest {
    @NotBlank(message = "Název poptávky je povinný")
    @Size(max = 150, message = "Název nesmí překročit 150 znaků")
    private String title;

    @Schema(description = "Id typů služeb z číselníku /api/service-types, alespoň jedno")
    @NotEmpty(message = "Vyberte alespoň jeden typ služby")
    private List<Long> serviceTypeIds; // ID ze seznamu, ne celý objekt

    @Size(max = 2000, message = "Popis nesmí překročit 2000 znaků")
    private String description;

    @Schema(description = "Požadované datum realizace - nesmí být v minulosti", example = "2026-06-01")
    @NotNull(message = "Požadované datum realizace je povinné")
    @FutureOrToday(message = "Požadované datum realizace nesmí být v minulosti")
    private LocalDate desiredDate;
}
