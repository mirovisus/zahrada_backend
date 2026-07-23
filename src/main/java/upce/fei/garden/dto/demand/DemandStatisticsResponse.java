package upce.fei.garden.dto.demand;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import upce.fei.garden.model.enums.DemandStatus;

/**
 * Přehled poptávky vlastníka s počtem přijatých návrhů.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DemandStatisticsResponse {
    private Long demandId;
    private String title;
    private DemandStatus status;
    private Long proposalCount;
}
