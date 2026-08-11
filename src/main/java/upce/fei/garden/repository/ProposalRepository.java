package upce.fei.garden.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import upce.fei.garden.model.Proposal;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.model.enums.ProposalStatus;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findAllByDemandId(Long demandId);

    // pravidlo "poptavku nelze menit, pokud uz existuje navrh"
    boolean existsByDemandId(Long demandId);

    // pravidlo "jeden zahradnik - jeden navrh na poptavku"
    boolean existsByDemandIdAndWorkerId(Long demandId, Long workerId);

    // overeni, ze prave tento zahradnik ma na poptavku prijaty navrh (pro odeslani reportu)
    boolean existsByDemandIdAndWorkerIdAndStatus(Long demandId, Long workerId, ProposalStatus status);

    // ziskani zahradnika s prijatym navrhem na poptavku (pro prirazeni hodnoceni pri prijeti prace)
    Optional<Proposal> findByDemandIdAndStatus(Long demandId, ProposalStatus status);

    Page<Proposal> findAllByWorkerId(Long workerId, Pageable pageable);

    // pro mazani uctu zahradnika (ProfileService#deleteMyAccount) - bez strankovani
    List<Proposal> findAllByWorkerId(Long workerId);

    // pro overeni vlastnictvi navrhu zahradnikem (withdraw)
    Optional<Proposal> findByIdAndWorkerId(Long id, Long workerId);

    // pro blokaci smazani uctu zahradnika s rozpracovanou zakazkou (ProfileService#deleteMyAccount)
    boolean existsByWorkerIdAndStatusAndDemandStatusIn(Long workerId, ProposalStatus status, List<DemandStatus> demandStatuses);
}
