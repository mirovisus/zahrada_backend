package upce.fei.garden.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import upce.fei.garden.model.Proposal;

import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findAllByDemandId(Long demandId);

    // pravidlo "poptavku nelze menit, pokud uz existuje navrh"
    boolean existsByDemandId(Long demandId);

    Page<Proposal> findAllByWorkerId(Long workerId, Pageable pageable);
}
