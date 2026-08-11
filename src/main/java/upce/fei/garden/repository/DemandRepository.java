package upce.fei.garden.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.model.enums.ProposalStatus;

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

    // pro blokaci smazani uctu vlastnika s rozpracovanymi zakazkami (ProfileService#deleteMyAccount)
    boolean existsByGardenOwnerIdAndStatusIn(Long ownerId, List<DemandStatus> statuses);

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

    /**
     * Vrátí zakázky daného zahradníka - poptávky, u kterých má tento zahradník návrh ve stavu
     * {@link ProposalStatus#SCHVALEN} a poptávka je v jednom ze zadaných stavů (typicky
     * {@code ZAPLACENA}, {@code PRACE_DOKONCENY}, {@code PRACE_SCHVALENY}).
     * <p>
     * Záměrně složitější dotaz - jedním JOIN na {@code proposals} s podmínkou přímo v ON klauzuli
     * (zahradník + stav návrhu) se zároveň získá cena z přijatého návrhu a LEFT JOIN na
     * {@code workReport} řekne, zda už zahradník k zakázce odeslal report.
     */
    @Query("""
            SELECT d.id AS demandId, d.title AS title, d.description AS description,
                   g.gardenName AS gardenName, g.address.city AS city, g.address.street AS street,
                   g.address.houseNumber AS houseNumber, g.mainPhotoUrl AS mainPhotoUrl,
                   d.status AS status, p.price AS price, d.createdAt AS createdAt,
                   CASE WHEN wr.id IS NOT NULL THEN true ELSE false END AS reportSubmitted
            FROM Demand d
            JOIN d.garden g
            JOIN d.proposals p ON p.worker.id = :workerId AND p.status = :proposalStatus
            LEFT JOIN d.workReport wr
            WHERE d.status IN :statuses
            ORDER BY d.createdAt DESC
            """)
    List<WorkerJobProjection> findActiveJobsForWorker(
            @Param("workerId") Long workerId,
            @Param("proposalStatus") ProposalStatus proposalStatus,
            @Param("statuses") List<DemandStatus> statuses);
}
