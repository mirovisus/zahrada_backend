package upce.fei.garden.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upce.fei.garden.dto.proposal.CreateProposalRequest;
import upce.fei.garden.dto.proposal.ProposalSummary;
import upce.fei.garden.dto.proposal.RequestChangesRequest;
import upce.fei.garden.exception.ConflictException;
import upce.fei.garden.exception.NotFoundException;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.Owner;
import upce.fei.garden.model.Proposal;
import upce.fei.garden.model.ProposalComment;
import upce.fei.garden.model.Worker;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.model.enums.ProposalStatus;
import upce.fei.garden.repository.DemandRepository;
import upce.fei.garden.repository.ProposalCommentRepository;
import upce.fei.garden.repository.ProposalRepository;
import upce.fei.garden.security.CurrentUserService;

import java.util.Arrays;
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
    private final ProposalCommentRepository proposalCommentRepository;
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
        ensureStatus(proposal, "přijmout", ProposalStatus.NOVY);

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
        ensureStatus(proposal, "zamítnout", ProposalStatus.NOVY);

        proposal.setStatus(ProposalStatus.ZAMITNUT);
        Proposal saved = proposalRepository.save(proposal);

        log.info("Zamítnut návrh: id={}, demandId={}, ownerId={}",
                proposalId, proposal.getDemand().getId(), owner.getId());

        return ProposalMapper.toSummary(saved);
    }

    /**
     * Odvolá vlastní návrh zahradníka, pokud je ve stavu {@link ProposalStatus#NOVY} nebo
     * {@link ProposalStatus#UPRAVY_POZADOVANY} – zahradník tak může návrh stáhnout i poté,
     * co o něm vlastník požádal o úpravy, aniž by musel čekat na jejich vypracování.
     *
     * @throws NotFoundException pokud návrh neexistuje nebo nepatří přihlášenému zahradníkovi
     * @throws ConflictException pokud návrh není ve stavu {@link ProposalStatus#NOVY} ani
     *                           {@link ProposalStatus#UPRAVY_POZADOVANY}
     */
    @Transactional
    public void withdraw(Long proposalId) {
        Worker worker = currentUserService.getCurrentWorker();
        Proposal proposal = proposalRepository.findByIdAndWorkerId(proposalId, worker.getId())
                .orElseThrow(() -> new NotFoundException("Návrh s id " + proposalId + " nebyl nalezen."));
        ensureStatus(proposal, "odvolat", ProposalStatus.NOVY, ProposalStatus.UPRAVY_POZADOVANY);

        proposalRepository.delete(proposal);
        log.info("Odvolán návrh: id={}, workerId={}", proposalId, worker.getId());
    }

    /**
     * Požádá jménem vlastníka poptávky o úpravu návrhu – uloží jeho komentář s požadovanými
     * změnami jako {@link ProposalComment} a přesune návrh do stavu
     * {@link ProposalStatus#UPRAVY_POZADOVANY}, aby jej zahradník mohl přepracovat přes
     * {@link #update(Long, CreateProposalRequest)}.
     *
     * @throws NotFoundException pokud návrh neexistuje nebo jeho poptávka nepatří přihlášenému vlastníkovi
     * @throws ConflictException pokud návrh není ve stavu {@link ProposalStatus#NOVY}
     */
    @Transactional
    public ProposalSummary requestChanges(Long proposalId, RequestChangesRequest request) {
        Owner owner = currentUserService.getCurrentOwner();
        Proposal proposal = findOwnedProposal(proposalId, owner);
        ensureStatus(proposal, "požádat o úpravy", ProposalStatus.NOVY);

        ProposalComment comment = new ProposalComment();
        comment.setProposal(proposal);
        comment.setAuthor(owner);
        comment.setText(request.getComment());
        proposalCommentRepository.save(comment);
        proposal.getComments().add(comment);

        proposal.setStatus(ProposalStatus.UPRAVY_POZADOVANY);
        Proposal saved = proposalRepository.save(proposal);

        log.info("Vyžádány úpravy návrhu: id={}, demandId={}, ownerId={}",
                proposalId, proposal.getDemand().getId(), owner.getId());

        return ProposalMapper.toSummary(saved);
    }

    /**
     * Přepracuje vlastní návrh zahradníka poté, co o něj vlastník požádal
     * ({@link #requestChanges(Long, RequestChangesRequest)}) – aktualizuje cenu a popis a vrátí
     * návrh zpět do stavu {@link ProposalStatus#NOVY}, aby o něm vlastník mohl znovu rozhodnout.
     *
     * @throws NotFoundException pokud návrh neexistuje nebo nepatří přihlášenému zahradníkovi
     * @throws ConflictException pokud návrh není ve stavu {@link ProposalStatus#UPRAVY_POZADOVANY}
     */
    @Transactional
    public ProposalSummary update(Long proposalId, CreateProposalRequest request) {
        Worker worker = currentUserService.getCurrentWorker();
        Proposal proposal = proposalRepository.findByIdAndWorkerId(proposalId, worker.getId())
                .orElseThrow(() -> new NotFoundException("Návrh s id " + proposalId + " nebyl nalezen."));
        ensureStatus(proposal, "upravit", ProposalStatus.UPRAVY_POZADOVANY);

        ProposalMapper.updateEntity(proposal, request);
        proposal.setStatus(ProposalStatus.NOVY);
        Proposal saved = proposalRepository.save(proposal);

        log.info("Přepracován návrh po žádosti o úpravy: id={}, workerId={}", proposalId, worker.getId());

        return ProposalMapper.toSummary(saved);
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

    private void ensureStatus(Proposal proposal, String action, ProposalStatus... allowedStatuses) {
        if (Arrays.stream(allowedStatuses).noneMatch(status -> status == proposal.getStatus())) {
            log.warn("Pokus o {} návrhu v nepovoleném stavu: proposalId={}, status={}",
                    action, proposal.getId(), proposal.getStatus());
            throw new ConflictException("Návrh nelze " + action + ", protože není ve vyžadovaném stavu.");
        }
    }
}
