package upce.fei.garden.dto.demand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.model.enums.DemandUrgency;

/**
 * Přehledová karta poptávky – název zahrady, stav a zkrácený popis.
 */
@Schema(description = "Přehledová karta poptávky - používá se v seznamu poptávek vlastníka i ve veřejném katalogu")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DemandSummary {
    private Long id;
    private String gardenName;
    private String descriptionPreview;
    private DemandUrgency urgency;

    @Schema(description = "Čitelný český popis naléhavosti, pro přímé zobrazení na frontendu")
    private String urgencyLabel;

    private DemandStatus status;
    // TODO: int proposalCount
}
