package upce.fei.garden.dto.workreport;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import upce.fei.garden.dto.dashboard.GardenSummary;
import upce.fei.garden.model.enums.DemandStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Overview card of a job in the logged-in worker's dashboard - a demand with their accepted
 * proposal, in status Approved or later.
 */
@Schema(description = "Overview card of a worker's job - a demand with their accepted proposal in status Approved or later")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WorkerJobSummary {
    private Long demandId;
    private String title;
    private String description;
    private GardenSummary garden;
    private DemandStatus status;
    private BigDecimal price;
    private LocalDateTime createdAt;

    @Schema(description = "True, pokud už zahradník k této zakázce odeslal report o provedených pracích")
    private boolean reportSubmitted;
}
