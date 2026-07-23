package upce.fei.garden.dto.demand;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import upce.fei.garden.dto.dashboard.GardenSummary;
import upce.fei.garden.model.enums.DemandStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detail poptávky – kompletní informace včetně služeb, stavu a přehledu zahrady.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DemandDetailResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDate desiredDate;
    private LocalDateTime createdAt;
    private DemandStatus status;
    private List<String> serviceTypeNames;
    private GardenSummary garden;
    private boolean hasProposals;
}
