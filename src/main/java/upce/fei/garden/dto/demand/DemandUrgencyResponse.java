package upce.fei.garden.dto.demand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Položka číselníku naléhavosti realizace poptávky.
 */
@Schema(description = "Položka číselníku naléhavosti realizace poptávky")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DemandUrgencyResponse {
    @Schema(description = "Název hodnoty enumu, používá se jako id při vytváření poptávky", example = "DO_TYDNE")
    private String id;

    @Schema(description = "Čitelný český popis naléhavosti", example = "Do týdne")
    private String label;
}
