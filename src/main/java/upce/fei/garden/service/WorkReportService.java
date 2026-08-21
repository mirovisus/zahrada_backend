package upce.fei.garden.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upce.fei.garden.dto.workreport.CreateWorkReport;
import upce.fei.garden.dto.workreport.WorkReportResponse;
import upce.fei.garden.dto.workreport.WorkerJobSummary;
import upce.fei.garden.exception.ConflictException;
import upce.fei.garden.exception.NotFoundException;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.WorkReport;
import upce.fei.garden.model.Worker;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.model.enums.ProposalStatus;
import upce.fei.garden.repository.DemandRepository;
import upce.fei.garden.repository.ProposalRepository;
import upce.fei.garden.repository.WorkReportRepository;
import upce.fei.garden.security.CurrentUserService;

import java.util.List;

/**
 * Kabinet zahradníka pro realizaci zakázek – přehled schválených poptávek s jeho přijatým návrhem
 * a odeslání reportu o dokončení prací (BPMN proces 06).
 * <p>
 * Model procesu má jen jeden mezistav mezi přijetím návrhu a dokončením: {@link DemandStatus#SCHVALENA}
 * zároveň znamená "návrh přijat, zahradník může pracovat" – žádné tlačítko/stav "zahájit práce"
 * neexistuje. Jakmile zahradník odešle report, poptávka přejde rovnou do
 * {@link DemandStatus#PRACE_DOKONCENY}.
 * <p>
 * Přístup k reportu cizí zakázky (zahradník nemá na poptávku přijatý návrh) se hlásí jako
 * {@link NotFoundException} (HTTP 404), nikoliv jako zákaz přístupu (HTTP 403) – stejná konvence
 * jako v {@link DemandService} a {@link ProposalService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkReportService {

    private static final List<DemandStatus> ACTIVE_JOB_STATUSES =
            List.of(DemandStatus.SCHVALENA, DemandStatus.PRACE_DOKONCENY, DemandStatus.PRACE_SCHVALENY);

    private final DemandRepository demandRepository;
    private final ProposalRepository proposalRepository;
    private final WorkReportRepository workReportRepository;
    private final CurrentUserService currentUserService;

    /**
     * Vrátí zakázky přihlášeného zahradníka – poptávky, na které měl přijatý návrh a jsou už
     * schválené nebo dále v realizaci ({@link #ACTIVE_JOB_STATUSES}).
     */
    @Transactional(readOnly = true)
    public List<WorkerJobSummary> getMyJobs() {
        Worker worker = currentUserService.getCurrentWorker();
        return demandRepository
                .findActiveJobsForWorker(worker.getId(), ProposalStatus.SCHVALEN, ACTIVE_JOB_STATUSES)
                .stream()
                .map(WorkReportMapper::toJobSummary)
                .toList();
    }

    /**
     * Odešle report o dokončených pracích k dané poptávce jménem přihlášeného zahradníka a v rámci
     * jedné transakce přesune poptávku ze stavu {@link DemandStatus#SCHVALENA} do
     * {@link DemandStatus#PRACE_DOKONCENY}.
     *
     * @throws NotFoundException pokud poptávka neexistuje nebo na ni přihlášený zahradník nemá
     *                           přijatý návrh ({@link ProposalStatus#SCHVALEN})
     * @throws ConflictException pokud poptávka není ve stavu {@link DemandStatus#SCHVALENA} –
     *                           to zahrnuje i opakované odeslání reportu, protože po prvním
     *                           odeslání už poptávka ve stavu {@code SCHVALENA} není
     */
    @Transactional
    public WorkReportResponse submitReport(Long demandId, CreateWorkReport request) {
        Worker worker = currentUserService.getCurrentWorker();
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new NotFoundException("Poptávka s id " + demandId + " nebyla nalezena."));

        if (!proposalRepository.existsByDemandIdAndWorkerIdAndStatus(demandId, worker.getId(), ProposalStatus.SCHVALEN)) {
            log.warn("Pokus o odeslání reportu k poptávce bez přijatého návrhu: demandId={}, workerId={}",
                    demandId, worker.getId());
            throw new NotFoundException("Poptávka s id " + demandId + " nebyla nalezena.");
        }

        if (demand.getStatus() != DemandStatus.SCHVALENA) {
            log.warn("Pokus o odeslání reportu mimo stav SCHVALENA: demandId={}, status={}, workerId={}",
                    demandId, demand.getStatus(), worker.getId());
            throw new ConflictException("Report nelze odeslat, protože poptávka není ve stavu Schválena.");
        }

        WorkReport report = WorkReportMapper.toEntity(request, demand, worker);
        WorkReport saved = workReportRepository.save(report);

        demand.setStatus(DemandStatus.PRACE_DOKONCENY);
        demandRepository.save(demand);

        log.info("Odeslán report o pracích: id={}, demandId={}, workerId={}", saved.getId(), demandId, worker.getId());

        return WorkReportMapper.toResponse(saved);
    }
}
