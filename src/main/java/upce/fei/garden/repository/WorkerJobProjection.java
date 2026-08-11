package upce.fei.garden.repository;

import upce.fei.garden.model.enums.DemandStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Projekce poptávky s přijatým návrhem konkrétního zahradníka - viz
 * {@link DemandRepository#findActiveJobsForWorker}.
 */
public interface WorkerJobProjection {
    Long getDemandId();
    String getTitle();
    String getDescription();
    String getGardenName();
    String getCity();
    String getStreet();
    String getHouseNumber();
    String getMainPhotoUrl();
    DemandStatus getStatus();
    BigDecimal getPrice();
    LocalDateTime getCreatedAt();
    Boolean getReportSubmitted();
}
