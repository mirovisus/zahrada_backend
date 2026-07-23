package upce.fei.garden.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.enums.DemandStatus;

import java.util.List;
import java.util.Optional;

/**
 * Poznámka: pro filtrování podle města (přes {@link org.springframework.data.jpa.domain.Specification})
 * je cesta k atributu {@code garden.address.city}, protože Address je @Embeddable v rámci Garden.
 */
public interface DemandRepository extends JpaRepository<Demand, Long>, JpaSpecificationExecutor<Demand> {
    Page<Demand> findAllByGardenId(Long gardenId, Pageable pageable);

    Page<Demand> findAllByGardenOwnerId(Long ownerId, Pageable pageable);

    Page<Demand> findAllByGardenOwnerIdAndStatus(Long ownerId, DemandStatus status, Pageable pageable);

    // pro overeni vlastnictvi poptavky pres zahradu
    Optional<Demand> findByIdAndGardenOwnerId(Long id, Long ownerId);

    /**
     * Vrátí přehled poptávek daného vlastníka spolu s počtem návrhů u každé z nich.
     * Používá LEFT JOIN na proposals, aby se do výsledku dostaly i poptávky bez jediného návrhu
     * (COUNT pak vrátí 0), a GROUP BY podle poptávky pro agregaci počtu návrhů.
     */
    @Query("""
            SELECT d.id AS demandId, d.title AS title, d.status AS status, COUNT(p) AS proposalCount
            FROM Demand d LEFT JOIN d.proposals p
            WHERE d.garden.owner.id = :ownerId
            GROUP BY d.id, d.title, d.status
            """)
    List<DemandWithProposalCount> findDemandsWithProposalCountByOwnerId(@Param("ownerId") Long ownerId);
}
