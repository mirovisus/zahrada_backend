package upce.fei.garden.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import upce.fei.garden.model.WorkReport;

import java.util.Optional;

public interface WorkReportRepository extends JpaRepository<WorkReport, Long> {
    Optional<WorkReport> findByDemandId(Long demandId);
}
