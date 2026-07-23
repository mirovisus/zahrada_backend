package upce.fei.garden.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upce.fei.garden.dto.demand.CreateDemandRequest;
import upce.fei.garden.dto.demand.DemandDetailResponse;
import upce.fei.garden.dto.demand.DemandStatisticsResponse;
import upce.fei.garden.dto.demand.DemandSummary;
import upce.fei.garden.exception.ConflictException;
import upce.fei.garden.exception.NotFoundException;
import upce.fei.garden.exception.ValidationException;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.Garden;
import upce.fei.garden.model.Owner;
import upce.fei.garden.model.ServiceType;
import upce.fei.garden.model.User;
import upce.fei.garden.model.Worker;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.repository.DemandRepository;
import upce.fei.garden.repository.GardenRepository;
import upce.fei.garden.repository.ProposalRepository;
import upce.fei.garden.repository.ServiceTypeRepository;
import upce.fei.garden.security.CurrentUserService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Správa poptávek (CRUD pro vlastníky) a veřejný katalog poptávek pro zahradníky.
 * &lt;p&gt;
 * Vlastnictví poptávky se vždy odvozuje od zahrady, ke které patří ({@code demand.garden.owner}),
 * a ta zase od aktuálně přihlášeného uživatele přes {@link CurrentUserService#getCurrentOwner()}.
 * Přístup vlastníka k cizí poptávce je hlášen jako {@link NotFoundException} (HTTP 404), nikoliv
 * jako zákaz přístupu (HTTP 403) – nechceme cizímu uživateli prozrazovat, že daný záznam vůbec
 * existuje (stejná konvence jako v {@link GardenService}).
 * &lt;p&gt;
 * Poptávku, ke které už existuje alespoň jeden návrh ({@link ProposalRepository#existsByDemandId}),
 * nelze upravit ani smazat – takový pokus je hlášen jako {@link ConflictException} (HTTP 409).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandService {

    private final DemandRepository demandRepository;
    private final GardenRepository gardenRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final ProposalRepository proposalRepository;
    private final CurrentUserService currentUserService;

    /**
     * Vrátí stránkovaný seznam poptávek napříč všemi zahradami aktuálně přihlášeného vlastníka,
     * volitelně omezený na konkrétní {@link DemandStatus}.
     */
    @Transactional(readOnly = true)
    public Page<DemandSummary> getMyDemands(Pageable pageable, DemandStatus status) {
        Owner owner = currentUserService.getCurrentOwner();
        Page<Demand> demands = status == null
                ? demandRepository.findAllByGardenOwnerId(owner.getId(), pageable)
                : demandRepository.findAllByGardenOwnerIdAndStatus(owner.getId(), status, pageable);
        return demands.map(DemandMapper::toSummary);
    }

    /**
     * Vrátí stránkovaný seznam poptávek dané zahrady, pokud zahrada patří aktuálně
     * přihlášenému vlastníkovi.
     *
     * @throws NotFoundException pokud zahrada neexistuje nebo nepatří přihlášenému vlastníkovi
     */
    @Transactional(readOnly = true)
    public Page<DemandSummary> getByGarden(Long gardenId, Pageable pageable) {
        Owner owner = currentUserService.getCurrentOwner();
        findOwnedGarden(gardenId, owner);
        return demandRepository.findAllByGardenId(gardenId, pageable).map(DemandMapper::toSummary);
    }

    /**
     * Vrátí detail poptávky podle id. Vlastník vidí pouze poptávky svých zahrad, zahradník
     * smí zobrazit libovolnou poptávku, ale pouze pokud je ve stavu {@link DemandStatus#NOVA}
     * (tedy je součástí veřejného katalogu) – ostatní stavy pro něj nejsou relevantní a jejich
     * existenci mu nechceme prozrazovat.
     *
     * @throws NotFoundException pokud poptávka neexistuje nebo k ní přihlášený uživatel nemá přístup
     */
    @Transactional(readOnly = true)
    public DemandDetailResponse getById(Long id) {
        User user = currentUserService.getCurrentUser();
        Demand demand = demandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Poptávka s id " + id + " nebyla nalezena."));

        if (user instanceof Owner owner && !demand.getGarden().getOwner().getId().equals(owner.getId())) {
            log.warn("Pokus o přístup k cizí poptávce: demandId={}, ownerId={}", id, owner.getId());
            throw new NotFoundException("Poptávka s id " + id + " nebyla nalezena.");
        }
        if (user instanceof Worker worker && demand.getStatus() != DemandStatus.NOVA) {
            log.warn("Pokus zahradníka o přístup k nedostupné poptávce: demandId={}, workerId={}, status={}",
                    id, worker.getId(), demand.getStatus());
            throw new NotFoundException("Poptávka s id " + id + " nebyla nalezena.");
        }

        boolean hasProposals = proposalRepository.existsByDemandId(id);
        return DemandMapper.toDetailResponse(demand, hasProposals);
    }

    /**
     * Vytvoří novou poptávku pro zahradu aktuálně přihlášeného vlastníka. Poptávka je vytvořena
     * ve stavu {@link DemandStatus#NOVA}.
     *
     * @throws NotFoundException   pokud zahrada neexistuje nebo nepatří přihlášenému vlastníkovi
     * @throws ValidationException pokud některé z {@code serviceTypeIds} neodpovídá existujícímu typu služby
     */
    @Transactional
    public DemandDetailResponse create(Long gardenId, CreateDemandRequest request) {
        Owner owner = currentUserService.getCurrentOwner();
        Garden garden = findOwnedGarden(gardenId, owner);
        List<ServiceType> serviceTypes = resolveServiceTypes(request.getServiceTypeIds());

        Demand demand = DemandMapper.toEntity(request, garden, serviceTypes);
        Demand saved = demandRepository.save(demand);
        log.info("Vytvořena poptávka: id={}, gardenId={}, ownerId={}", saved.getId(), gardenId, owner.getId());

        return DemandMapper.toDetailResponse(saved, false);
    }

    /**
     * Aktualizuje poptávku, pokud patří aktuálně přihlášenému vlastníkovi.
     *
     * @throws NotFoundException   pokud poptávka neexistuje nebo nepatří přihlášenému vlastníkovi
     * @throws ConflictException   pokud k poptávce už existuje alespoň jeden návrh
     * @throws ValidationException pokud některé z {@code serviceTypeIds} neodpovídá existujícímu typu služby
     */
    @Transactional
    public DemandDetailResponse update(Long id, CreateDemandRequest request) {
        Owner owner = currentUserService.getCurrentOwner();
        Demand demand = findOwnedDemand(id, owner);
        ensureNoProposals(demand, "upravit");

        List<ServiceType> serviceTypes = resolveServiceTypes(request.getServiceTypeIds());
        DemandMapper.updateEntity(demand, request, serviceTypes);

        Demand saved = demandRepository.save(demand);
        log.info("Aktualizována poptávka: id={}, ownerId={}", saved.getId(), owner.getId());

        return DemandMapper.toDetailResponse(saved, false);
    }

    /**
     * Smaže poptávku, pokud patří aktuálně přihlášenému vlastníkovi.
     *
     * @throws NotFoundException pokud poptávka neexistuje nebo nepatří přihlášenému vlastníkovi
     * @throws ConflictException pokud k poptávce už existuje alespoň jeden návrh
     */
    @Transactional
    public void delete(Long id) {
        Owner owner = currentUserService.getCurrentOwner();
        Demand demand = findOwnedDemand(id, owner);
        ensureNoProposals(demand, "smazat");

        demandRepository.delete(demand);
        log.info("Smazána poptávka: id={}, ownerId={}", id, owner.getId());
    }

    /**
     * Vrátí stránkovaný veřejný katalog poptávek pro zahradníky.
     * &lt;p&gt;
     * Katalog vždy obsahuje pouze poptávky ve stavu {@link DemandStatus#NOVA} – to je jediný stav,
     * ve kterém poptávka ještě "hledá" zahradníka. Filtry ({@code city}, {@code serviceTypeIds},
     * {@code search}) jsou volitelné a navzájem se kombinují logickým AND; v rámci jednoho filtru
     * (např. více {@code serviceTypeIds}) se použije logika "poptávka obsahuje alespoň jeden
     * z vybraných typů služeb".
     * &lt;p&gt;
     * Dotaz je sestaven přes {@link Specification} v {@link DemandSpecifications#catalog}, protože
     * počet aktivních filtrů se liší request od requestu (statický JPQL/derived-query dotaz by
     * musel řešit kombinatoriku {@code null}/ne-{@code null} parametrů). Filtr podle města jde po
     * cestě {@code garden.address.city} – {@link upce.fei.garden.model.Address} je {@code @Embeddable}
     * uvnitř {@link Garden}, takže se v Criteria API adresuje jako vnořená cesta, ne přes join.
     * Filtr podle typů služeb používá {@code join("serviceTypes")} (vazba {@code @ManyToMany}) a
     * nastavuje na dotazu {@code distinct(true)}, aby se poptávka s více odpovídajícími typy služeb
     * ve stránkovaném výsledku neobjevila vícekrát. Fulltextové hledání porovnává podřetězec
     * (case-insensitive {@code LIKE}) v {@code title} i {@code description}.
     */
    @Transactional(readOnly = true)
    public Page<DemandSummary> getCatalog(String city, List<Long> serviceTypeIds, String search, Pageable pageable) {
        Specification<Demand> specification = DemandSpecifications.catalog(city, serviceTypeIds, search);
        return demandRepository.findAll(specification, pageable).map(DemandMapper::toSummary);
    }

    /**
     * Vrátí přehled všech poptávek aktuálně přihlášeného vlastníka spolu s počtem přijatých návrhů
     * u každé z nich (přes projekci {@link upce.fei.garden.repository.DemandWithProposalCount}).
     */
    @Transactional(readOnly = true)
    public List<DemandStatisticsResponse> getMyDemandsWithProposalCount() {
        Owner owner = currentUserService.getCurrentOwner();
        return demandRepository.findDemandsWithProposalCountByOwnerId(owner.getId()).stream()
                .map(DemandMapper::toStatistics)
                .toList();
    }

    private Garden findOwnedGarden(Long gardenId, Owner owner) {
        return gardenRepository.findByIdAndOwnerId(gardenId, owner.getId())
                .orElseThrow(() -> {
                    log.warn("Pokus o přístup k cizí nebo neexistující zahradě: id={}, ownerId={}",
                            gardenId, owner.getId());
                    return new NotFoundException("Zahrada s id " + gardenId + " nebyla nalezena.");
                });
    }

    private Demand findOwnedDemand(Long id, Owner owner) {
        return demandRepository.findByIdAndGardenOwnerId(id, owner.getId())
                .orElseThrow(() -> {
                    log.warn("Pokus o přístup k cizí poptávce: demandId={}, ownerId={}", id, owner.getId());
                    return new NotFoundException("Poptávka s id " + id + " nebyla nalezena.");
                });
    }

    private void ensureNoProposals(Demand demand, String action) {
        if (proposalRepository.existsByDemandId(demand.getId())) {
            log.warn("Pokus o {} poptávky, ke které již existuje návrh: demandId={}", action, demand.getId());
            throw new ConflictException(
                    "Poptávku nelze " + action + ", protože k ní již existuje návrh od zahradníka.");
        }
    }

    private List<ServiceType> resolveServiceTypes(List<Long> serviceTypeIds) {
        List<ServiceType> serviceTypes = serviceTypeRepository.findAllByIdIn(serviceTypeIds);
        if (serviceTypes.size() != serviceTypeIds.size()) {
            Set<Long> foundIds = serviceTypes.stream().map(ServiceType::getId).collect(Collectors.toSet());
            List<Long> missingIds = serviceTypeIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new ValidationException("Neplatné id typu služby: " + missingIds);
        }
        return serviceTypes;
    }
}
