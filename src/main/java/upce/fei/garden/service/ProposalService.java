package upce.fei.garden.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upce.fei.garden.dto.proposal.CreateProposalRequest;
import upce.fei.garden.dto.proposal.ProposalSummary;
import upce.fei.garden.exception.ConflictException;
import upce.fei.garden.exception.NotFoundException;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.Owner;
import upce.fei.garden.model.Proposal;
import upce.fei.garden.model.Worker;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.model.enums.ProposalStatus;
import upce.fei.garden.repository.DemandRepository;
import upce.fei.garden.repository.ProposalRepository;
import upce.fei.garden.security.CurrentUserService;

import java.util.List;

/**
 * Správa návrhů (nabídek) zahradníků na poptávky vlastníků.
 * <p>
 * Návrh smí podat pouze zahradník, a to jen na poptávku ve stavu {@link DemandStatus#NOVA} –
 * to je jediný stav, ve kterém poptávka ještě "hledá" zahradníka (viz {@link DemandService#getCatalog}).
 * Na jednu poptávku smí konkrétní zahradník podat nejvýš jeden návrh
 * ({@link ProposalRepository#existsByDemandIdAndWorkerId}); opakovaný pokus je hlášen jako
 * {@link ConflictException} (HTTP 409).
 * <p>
 * Přístup vlastníka k návrhům cizí poptávky nebo zahradníka k cizímu návrhu se hlásí jako
 * {@link NotFoundException} (HTTP 404), nikoliv jako zákaz přístupu (HTTP 403) – nechceme cizímu
 * uživateli prozrazovat, že daný záznam vůbec existuje (stejná konvence jako v {@link DemandService}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final DemandRepository demandRepository;
    private final CurrentUserService currentUserService;

    /**
     * Vytvoří nový návrh přihlášeného zahradníka na danou poptávku.
     *
     * @throws NotFoundException pokud poptávka neexistuje
     * @throws ConflictException pokud poptávka není ve stavu {@link DemandStatus#NOVA},
     *                           nebo pokud na ni zahradník už dříve podal návrh
     */
    @Transactional
    public ProposalSummary create(Long demandId, CreateProposalRequest request) {
        Worker worker = currentUserService.getCurrentWorker();
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new NotFoundException("Poptávka s id " + demandId + " nebyla nalezena."));

        if (demand.getStatus() != DemandStatus.NOVA) {
            log.warn("Pokus o návrh na poptávku mimo stav NOVA: demandId={}, status={}, workerId={}",
                    demandId, demand.getStatus(), worker.getId());
            throw new ConflictException("Na tuto poptávku již nelze podat návrh.");
        }

        if (proposalRepository.existsByDemandIdAndWorkerId(demandId, worker.getId())) {
            log.warn("Pokus o opakovaný návrh na poptávku: demandId={}, workerId={}", demandId, worker.getId());
            throw new ConflictException("Na tuto poptávku jste již podal návrh.");
        }

        Proposal proposal = ProposalMapper.toEntity(request, demand, worker);
        Proposal saved = proposalRepository.save(proposal);
        log.info("Vytvořen návrh: id={}, demandId={}, workerId={}", saved.getId(), demandId, worker.getId());

        return ProposalMapper.toSummary(saved);
    }

    /**
     * Vrátí seznam všech návrhů dané poptávky, pokud poptávka patří přihlášenému vlastníkovi.
     *
     * @throws NotFoundException pokud poptávka neexistuje nebo nepatří přihlášenému vlastníkovi
     */
    @Transactional(readOnly = true)
    public List<ProposalSummary> getByDemand(Long demandId) {
        Owner owner = currentUserService.getCurrentOwner();
        Demand demand = findOwnedDemand(demandId, owner);
        return proposalRepository.findAllByDemandId(demand.getId()).stream()
                .map(ProposalMapper::toSummary)
                .toList();
    }

    /**
     * Vrátí stránkovaný seznam návrhů podaných aktuálně přihlášeným zahradníkem.
     */
    @Transactional(readOnly = true)
    public Page<ProposalSummary> getMyProposals(Pageable pageable) {
        Worker worker = currentUserService.getCurrentWorker();
        return proposalRepository.findAllByWorkerId(worker.getId(), pageable).map(ProposalMapper::toSummary);
    }

    /**
     * Přijme návrh jménem vlastníka poptávky a spustí kaskádu změn stavů v rámci jedné transakce:
     * <ol>
     *     <li>přijímaný návrh přejde do stavu {@link ProposalStatus#SCHVALEN};</li>
     *     <li>všechny ostatní návrhy stejné poptávky (bez ohledu na jejich aktuální stav) přejdou
     *     do stavu {@link ProposalStatus#ZAMITNUT} – po výběru zahradníka už o ně vlastník nemá zájem;</li>
     *     <li>samotná poptávka přejde ze stavu {@link DemandStatus#NOVA} do {@link DemandStatus#SCHVALENA}.</li>
     * </ol>
     * Návrh lze přijmout pouze ve stavu {@link ProposalStatus#NOVY} – jakmile je jednou přijat nebo
     * zamítnut, jsou ostatní návrhy dané poptávky vždy {@link ProposalStatus#ZAMITNUT}, takže opakované
     * přijetí libovolného návrhu stejné poptávky je tímto automaticky zablokováno.
     *
     * @throws NotFoundException pokud návrh neexistuje nebo jeho poptávka nepatří přihlášenému vlastníkovi
     * @throws ConflictException pokud návrh není ve stavu {@link ProposalStatus#NOVY}
     */
    @Transactional
    public ProposalSummary accept(Long proposalId) {
        Owner owner = currentUserService.getCurrentOwner();
        Proposal proposal = findOwnedProposal(proposalId, owner);
        ensureNovy(proposal, "přijmout");

        Demand demand = proposal.getDemand();
        List<Proposal> allProposals = proposalRepository.findAllByDemandId(demand.getId());
        for (Proposal candidate : allProposals) {
            candidate.setStatus(candidate.getId().equals(proposal.getId())
                    ? ProposalStatus.SCHVALEN
                    : ProposalStatus.ZAMITNUT);
        }
        proposalRepository.saveAll(allProposals);

        demand.setStatus(DemandStatus.SCHVALENA);
        demandRepository.save(demand);

        log.info("Přijat návrh: id={}, demandId={}, ownerId={}", proposalId, demand.getId(), owner.getId());

        return ProposalMapper.toSummary(proposal);
    }

    /**
     * Zamítne návrh jménem vlastníka poptávky. Na rozdíl od {@link #accept(Long)} nemění stav
     * poptávky ani ostatních návrhů – vlastník tak může zamítat návrhy postupně, dokud si mezi
     * zbývajícími nevybere.
     *
     * @throws NotFoundException pokud návrh neexistuje nebo jeho poptávka nepatří přihlášenému vlastníkovi
     * @throws ConflictException pokud návrh není ve stavu {@link ProposalStatus#NOVY}
     */
    @Transactional
    public ProposalSummary reject(Long proposalId) {
        Owner owner = currentUserService.getCurrentOwner();
        Proposal proposal = findOwnedProposal(proposalId, owner);
        ensureNovy(proposal, "zamítnout");

        proposal.setStatus(ProposalStatus.ZAMITNUT);
        Proposal saved = proposalRepository.save(proposal);

        log.info("Zamítnut návrh: id={}, demandId={}, ownerId={}",
                proposalId, proposal.getDemand().getId(), owner.getId());

        return ProposalMapper.toSummary(saved);
    }

    /**
     * Odvolá vlastní návrh zahradníka, pokud je stále ve stavu {@link ProposalStatus#NOVY}.
     *
     * @throws NotFoundException pokud návrh neexistuje nebo nepatří přihlášenému zahradníkovi
     * @throws ConflictException pokud návrh není ve stavu {@link ProposalStatus#NOVY}
     */
    @Transactional
    public void withdraw(Long proposalId) {
        Worker worker = currentUserService.getCurrentWorker();
        Proposal proposal = proposalRepository.findByIdAndWorkerId(proposalId, worker.getId())
                .orElseThrow(() -> new NotFoundException("Návrh s id " + proposalId + " nebyl nalezen."));
        ensureNovy(proposal, "odvolat");

        proposalRepository.delete(proposal);
        log.info("Odvolán návrh: id={}, workerId={}", proposalId, worker.getId());
    }

    private Demand findOwnedDemand(Long demandId, Owner owner) {
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new NotFoundException("Poptávka s id " + demandId + " nebyla nalezena."));
        if (!demand.getGarden().getOwner().getId().equals(owner.getId())) {
            log.warn("Pokus o přístup k návrhům cizí poptávky: demandId={}, ownerId={}", demandId, owner.getId());
            throw new NotFoundException("Poptávka s id " + demandId + " nebyla nalezena.");
        }
        return demand;
    }

    private Proposal findOwnedProposal(Long proposalId, Owner owner) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new NotFoundException("Návrh s id " + proposalId + " nebyl nalezen."));
        if (!proposal.getDemand().getGarden().getOwner().getId().equals(owner.getId())) {
            log.warn("Pokus o přístup k cizímu návrhu: proposalId={}, ownerId={}", proposalId, owner.getId());
            throw new NotFoundException("Návrh s id " + proposalId + " nebyl nalezen.");
        }
        return proposal;
    }

    private void ensureNovy(Proposal proposal, String action) {
        if (proposal.getStatus() != ProposalStatus.NOVY) {
            log.warn("Pokus o {} návrhu mimo stav NOVY: proposalId={}, status={}",
                    action, proposal.getId(), proposal.getStatus());
            throw new ConflictException("Návrh nelze " + action + ", protože již není ve stavu Nový.");
        }
    }
}
