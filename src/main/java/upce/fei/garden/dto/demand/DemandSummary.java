package upce.fei.garden.dto.demand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import upce.fei.garden.model.enums.DemandStatus;

import java.time.LocalDate;

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
    private LocalDate desiredDate;
    private DemandStatus status;
    // TODO: int proposalCount
}
