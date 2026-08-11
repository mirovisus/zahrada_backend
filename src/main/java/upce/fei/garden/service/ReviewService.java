package upce.fei.garden.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upce.fei.garden.dto.demand.DemandDetailResponse;
import upce.fei.garden.dto.review.CreateReview;
import upce.fei.garden.exception.ConflictException;
import upce.fei.garden.exception.NotFoundException;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.Owner;
import upce.fei.garden.model.Proposal;
import upce.fei.garden.model.Review;
import upce.fei.garden.model.Worker;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.model.enums.ProposalStatus;
import upce.fei.garden.repository.DemandRepository;
import upce.fei.garden.repository.ProposalRepository;
import upce.fei.garden.repository.ReviewRepository;
import upce.fei.garden.repository.UserRepository;
import upce.fei.garden.security.CurrentUserService;

/**
 * Přijetí dokončené práce vlastníkem a jeho hodnocení zahradníka – poslední krok BPMN procesu 06.
 * <p>
 * Přijetí a hodnocení je jedna akce ({@link #acceptWork}) – model procesu nemá samostatný krok
 * "schválit práci" a "ohodnotit" zvlášť. Poptávka je tím pádem terminální: po přechodu do
 * {@link DemandStatus#PRACE_SCHVALENY} už žádná další akce nad ní není definována.
 * <p>
 * Přístup vlastníka k cizí poptávce se hlásí jako {@link NotFoundException} (HTTP 404), stejná
 * konvence jako v {@link DemandService}, {@link ProposalService} a {@link WorkReportService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final DemandRepository demandRepository;
    private final ProposalRepository proposalRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    /**
     * Přijme dokončenou práci a zároveň založí hodnocení zahradníka, jehož návrh byl na poptávce
     * přijat. V rámci jedné transakce: přesune poptávku do stavu {@link DemandStatus#PRACE_SCHVALENY},
     * uloží {@link Review} a přepočítá {@link Worker#getAverageRating()} jako průměr přes všechny
     * jeho hodnocení ({@link ReviewRepository#findAverageRatingByWorkerId}).
     *
     * @throws NotFoundException pokud poptávka neexistuje nebo nepatří přihlášenému vlastníkovi
     * @throws ConflictException pokud poptávka není ve stavu {@link DemandStatus#PRACE_DOKONCENY} –
     *                           to zahrnuje i opakované přijetí, protože po prvním přijetí už
     *                           poptávka ve stavu {@code PRACE_DOKONCENY} není
     */
    @Transactional
    public DemandDetailResponse acceptWork(Long demandId, CreateReview request) {
        Owner owner = currentUserService.getCurrentOwner();
        Demand demand = findOwnedDemand(demandId, owner);

        if (demand.getStatus() != DemandStatus.PRACE_DOKONCENY) {
            log.warn("Pokus o přijetí práce mimo stav PRACE_DOKONCENY: demandId={}, status={}, ownerId={}",
                    demandId, demand.getStatus(), owner.getId());
            throw new ConflictException("Práci nelze přijmout, protože poptávka není ve stavu Práce dokončeny.");
        }

        Proposal acceptedProposal = proposalRepository.findByDemandIdAndStatus(demandId, ProposalStatus.SCHVALEN)
                .orElseThrow(() -> new IllegalStateException(
                        "Poptávka s id " + demandId + " je ve stavu PRACE_DOKONCENY, ale nemá přijatý návrh."));
        Worker worker = acceptedProposal.getWorker();

        Review review = ReviewMapper.toEntity(request, demand, owner, worker);
        reviewRepository.save(review);
        // Demand.review je inverzni (mappedBy) strana vztahu - bez tohoto rucniho prirazeni by
        // demand.getReview() v ramci teto same transakce dal vracel puvodni (null) hodnotu
        demand.setReview(review);

        worker.setAverageRating(reviewRepository.findAverageRatingByWorkerId(worker.getId()));
        userRepository.save(worker);

        demand.setStatus(DemandStatus.PRACE_SCHVALENY);
        Demand saved = demandRepository.save(demand);

        log.info("Přijata práce a ohodnocen zahradník: demandId={}, workerId={}, ownerId={}, rating={}",
                demandId, worker.getId(), owner.getId(), request.getRating());

        boolean hasProposals = proposalRepository.existsByDemandId(demandId);
        return DemandMapper.toDetailResponse(saved, hasProposals);
    }

    private Demand findOwnedDemand(Long demandId, Owner owner) {
        return demandRepository.findByIdAndGardenOwnerId(demandId, owner.getId())
                .orElseThrow(() -> {
                    log.warn("Pokus o přístup k cizí poptávce: demandId={}, ownerId={}", demandId, owner.getId());
                    return new NotFoundException("Poptávka s id " + demandId + " nebyla nalezena.");
                });
    }
}
