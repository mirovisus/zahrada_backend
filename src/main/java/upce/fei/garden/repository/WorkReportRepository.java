package upce.fei.garden.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import upce.fei.garden.model.WorkReport;

import java.util.List;
import java.util.Optional;

public interface WorkReportRepository extends JpaRepository<WorkReport, Long> {
    Optional<WorkReport> findByDemandId(Long demandId);

    // pro mazani uctu zahradnika (ProfileService#deleteMyAccount)
    List<WorkReport> findAllByWorkerId(Long workerId);
}
