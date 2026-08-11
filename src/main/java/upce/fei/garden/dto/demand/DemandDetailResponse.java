package upce.fei.garden.dto.demand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import upce.fei.garden.dto.dashboard.GardenSummary;
import upce.fei.garden.dto.review.ReviewResponse;
import upce.fei.garden.dto.workreport.WorkReportResponse;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.model.enums.DemandUrgency;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detail poptávky – kompletní informace včetně služeb, stavu a přehledu zahrady.
 */
@Schema(description = "Detail poptávky - vlastník vidí vždy svou, zahradník jen ve stavu NOVA")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DemandDetailResponse {
    private Long id;
    private String title;
    private String description;
    private DemandUrgency urgency;

    @Schema(description = "Čitelný český popis naléhavosti, pro přímé zobrazení na frontendu")
    private String urgencyLabel;

    private LocalDateTime createdAt;
    private DemandStatus status;
    private List<String> serviceTypeNames;
    private GardenSummary garden;

    @Schema(description = "True, pokud na poptávku už existuje alespoň jeden návrh - pak ji nelze upravit ani smazat")
    private boolean hasProposals;

    @Schema(description = "Report zahradníka o dokončených pracích, nebo null, pokud ještě nebyl odeslán")
    private WorkReportResponse workReport;

    @Schema(description = "Hodnocení zahradníka vlastníkem, nebo null, dokud vlastník práci nepřijme")
    private ReviewResponse review;
}
