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
 * Přehledová karta zakázky v kabinetu přihlášeného zahradníka – poptávka, na kterou měl přijatý
 * návrh, a je tedy zaplacená nebo dále v realizaci.
 */
@Schema(description = "Přehledová karta zakázky zahradníka - poptávka s jeho přijatým návrhem ve stavu Zaplaceno a dále")
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
