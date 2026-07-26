package upce.fei.garden.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import upce.fei.garden.model.Proposal;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findAllByDemandId(Long demandId);

    // pravidlo "poptavku nelze menit, pokud uz existuje navrh"
    boolean existsByDemandId(Long demandId);

    // pravidlo "jeden zahradnik - jeden navrh na poptavku"
    boolean existsByDemandIdAndWorkerId(Long demandId, Long workerId);

    Page<Proposal> findAllByWorkerId(Long workerId, Pageable pageable);

    // pro overeni vlastnictvi navrhu zahradnikem (withdraw)
    Optional<Proposal> findByIdAndWorkerId(Long id, Long workerId);
}
