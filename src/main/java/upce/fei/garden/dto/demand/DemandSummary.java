package upce.fei.garden.dto.demand;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import upce.fei.garden.model.enums.DemandStatus;

import java.time.LocalDate;

/**
 * Přehledová karta poptávky – název zahrady, stav a zkrácený popis.
 */

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
