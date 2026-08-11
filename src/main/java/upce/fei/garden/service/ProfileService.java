package upce.fei.garden.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upce.fei.garden.dto.profile.OwnerProfileResponse;
import upce.fei.garden.dto.profile.UpdateOwnerProfileRequest;
import upce.fei.garden.dto.profile.UpdateWorkerProfileRequest;
import upce.fei.garden.dto.profile.WorkerProfileResponse;
import upce.fei.garden.exception.ConflictException;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.Garden;
import upce.fei.garden.model.Owner;
import upce.fei.garden.model.Proposal;
import upce.fei.garden.model.User;
import upce.fei.garden.model.Worker;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.model.enums.ProposalStatus;
import upce.fei.garden.repository.DemandRepository;
import upce.fei.garden.repository.GardenRepository;
import upce.fei.garden.repository.ProposalRepository;
import upce.fei.garden.repository.ReviewRepository;
import upce.fei.garden.repository.UserRepository;
import upce.fei.garden.repository.WorkReportRepository;
import upce.fei.garden.security.CurrentUserService;

import java.util.List;

/**
 * Správa profilu přihlášeného uživatele. Typ profilu (vlastník/zahradník) se vždy odvozuje
 * od skutečné třídy entity aktuálně přihlášeného uživatele (viz {@link CurrentUserService}).
 * <p>
 * E-mail lze při aktualizaci měnit, ale musí zůstat v systému unikátní – konflikt s jiným
 * uživatelem se hlásí jako {@link ConflictException} (HTTP 409). Heslo se mění pouze tehdy,
 * pokud je v požadavku vyplněno {@code newPassword}; prázdná hodnota ponechá stávající heslo
 * beze změny a v odpovědi se heslo nikdy nevrací.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    /**
     * Stavy poptávky, které blokují smazání účtu, protože zakázka je rozpracovaná (vybraný
     * zahradník na ní pracuje, nebo na platbu/dokončení teprve čeká).
     */
    private static final List<DemandStatus> BLOCKING_DEMAND_STATUSES =
            List.of(DemandStatus.SCHVALENA, DemandStatus.ZAPLACENA, DemandStatus.PRACE_DOKONCENY);

    private final UserRepository userRepository;
    private final GardenRepository gardenRepository;
    private final DemandRepository demandRepository;
    private final ProposalRepository proposalRepository;
    private final WorkReportRepository workReportRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;

    /**
     * Vrátí profil aktuálně přihlášeného uživatele. Podle skutečné třídy entity vrací buď
     * {@link OwnerProfileResponse}, nebo {@link WorkerProfileResponse}.
     */
    @Transactional(readOnly = true)
    public Object getMyProfile() {
        User user = currentUserService.getCurrentUser();
        if (user instanceof Owner owner) {
            return ProfileMapper.toResponse(owner);
        }
        if (user instanceof Worker worker) {
            return ProfileMapper.toResponse(worker);
        }
        throw new IllegalStateException("Neznámý typ uživatele: " + user.getClass());
    }

    /**
     * Aktualizuje profil aktuálně přihlášeného vlastníka zahrady.
     *
     * @throws ConflictException pokud je zadaný e-mail již použit jiným uživatelem
     */
    @Transactional
    public OwnerProfileResponse updateOwnerProfile(UpdateOwnerProfileRequest request) {
        Owner owner = currentUserService.getCurrentOwner();

        ensureEmailAvailable(owner, request.getEmail());

        owner.setFirstName(request.getFirstName());
        owner.setLastName(request.getLastName());
        owner.setEmail(request.getEmail());
        owner.setPhoneNumber(request.getPhoneNumber());
        owner.setAvatarUrl(request.getAvatarUrl());
        applyNewPassword(owner, request.getNewPassword());

        Owner saved = userRepository.save(owner);
        log.info("Aktualizován profil vlastníka: id={}", saved.getId());

        return ProfileMapper.toResponse(saved);
    }

    /**
     * Aktualizuje profil aktuálně přihlášeného zahradníka.
     *
     * @throws ConflictException pokud je zadaný e-mail již použit jiným uživatelem
     */
    @Transactional
    public WorkerProfileResponse updateWorkerProfile(UpdateWorkerProfileRequest request) {
        Worker worker = currentUserService.getCurrentWorker();

        ensureEmailAvailable(worker, request.getEmail());

        worker.setFirstName(request.getFirstName());
        worker.setLastName(request.getLastName());
        worker.setBio(request.getBio());
        worker.setEmail(request.getEmail());
        worker.setPhoneNumber(request.getPhoneNumber());
        worker.setAvatarUrl(request.getAvatarUrl());
        applyNewPassword(worker, request.getNewPassword());

        Worker saved = userRepository.save(worker);
        log.info("Aktualizován profil zahradníka: id={}", saved.getId());

        return ProfileMapper.toResponse(saved);
    }

    /**
     * Smaže účet aktuálně přihlášeného uživatele (vlastníka i zahradníka) včetně všech
     * souvisejících dat. Nevratná operace – provede se pouze pokud uživatel nemá žádnou
     * rozpracovanou zakázku (viz {@link #ensureOwnerHasNoActiveDemands} /
     * {@link #ensureWorkerHasNoActiveJob}), aby smazáním účtu nezmizela zakázka, na které
     * druhá strana právě pracuje nebo čeká na její dokončení.
     *
     * @throws ConflictException pokud má uživatel rozpracovanou zakázku
     */
    @Transactional
    public void deleteMyAccount() {
        User user = currentUserService.getCurrentUser();
        if (user instanceof Owner owner) {
            deleteOwnerAccount(owner);
            return;
        }
        if (user instanceof Worker worker) {
            deleteWorkerAccount(worker);
            return;
        }
        throw new IllegalStateException("Neznámý typ uživatele: " + user.getClass());
    }

    /**
     * Smaže účet vlastníka: nejprve všechny reporty prací a hodnocení navázané na jeho poptávky
     * (nejsou kaskádovány z {@link Demand}, musí se smazat explicitně, jinak by mazání poptávky
     * selhalo na cizím klíči), poté zahrady vlastníka (kaskádově smaže i jejich poptávky, návrhy
     * na ně a komentáře k návrhům – viz kaskády na {@link Garden#getDemands()} a
     * {@link Demand#getProposals()}) a nakonec samotný účet.
     * <p>
     * Zahrady se záměrně načítají znovu přes {@link GardenRepository#findAllByOwnerId(Long)}, ne
     * přes {@code owner.getGardens()} – entita z {@link CurrentUserService} pochází z relace
     * navázané ve filtru (mimo transakci této metody), takže její líné kolekce by při přístupu
     * skončily na {@code LazyInitializationException}.
     */
    private void deleteOwnerAccount(Owner owner) {
        if (demandRepository.existsByGardenOwnerIdAndStatusIn(owner.getId(), BLOCKING_DEMAND_STATUSES)) {
            log.warn("Pokus o smazání účtu vlastníka s rozpracovanou zakázkou: ownerId={}", owner.getId());
            throw new ConflictException("Účet nelze smazat, máte rozpracované zakázky. Nejprve je dokončete nebo zrušte.");
        }

        List<Garden> gardens = gardenRepository.findAllByOwnerId(owner.getId());
        for (Garden garden : gardens) {
            for (Demand demand : garden.getDemands()) {
                if (demand.getWorkReport() != null) {
                    workReportRepository.delete(demand.getWorkReport());
                }
                if (demand.getReview() != null) {
                    reviewRepository.delete(demand.getReview());
                }
            }
        }

        gardenRepository.deleteAll(gardens);
        for (Garden garden : gardens) {
            fileStorageService.delete(garden.getMainPhotoUrl());
        }

        userRepository.delete(owner);
        log.info("Smazán účet vlastníka: id={}, email={}", owner.getId(), owner.getEmail());
    }

    /**
     * Smaže účet zahradníka: jeho návrhy (kaskádově smaže i komentáře k nim – viz kaskáda na
     * {@link Proposal#getComments()}), jím odeslané reporty prací a hodnocení, která obdržel,
     * a nakonec samotný účet. Žádná z těchto vazeb ({@link Worker#getProposals()},
     * {@link Worker#getWorkReports()}, {@link Worker#getReviews()}) není kaskádována z {@link
     * Worker}, proto se maže explicitně (načteno znovu přes repository, ne přes lazy kolekce
     * detached entity z {@link CurrentUserService} – stejný důvod jako u {@link
     * #deleteOwnerAccount}); smazané reporty/hodnocení se mohou týkat i poptávek jiných (cizích)
     * vlastníků, u kterých zahradník práci již dokončil – to je záměr, poptávka samotná zůstává,
     * přijde jen o report/hodnocení tohoto zahradníka.
     */
    private void deleteWorkerAccount(Worker worker) {
        if (proposalRepository.existsByWorkerIdAndStatusAndDemandStatusIn(
                worker.getId(), ProposalStatus.SCHVALEN, BLOCKING_DEMAND_STATUSES)) {
            log.warn("Pokus o smazání účtu zahradníka s rozpracovanou zakázkou: workerId={}", worker.getId());
            throw new ConflictException("Účet nelze smazat, máte rozpracované zakázky.");
        }

        proposalRepository.deleteAll(proposalRepository.findAllByWorkerId(worker.getId()));
        workReportRepository.deleteAll(workReportRepository.findAllByWorkerId(worker.getId()));
        reviewRepository.deleteAll(reviewRepository.findAllByWorkerId(worker.getId()));

        userRepository.delete(worker);
        log.info("Smazán účet zahradníka: id={}, email={}", worker.getId(), worker.getEmail());
    }

    private void ensureEmailAvailable(User user, String requestedEmail) {
        if (requestedEmail.equalsIgnoreCase(user.getEmail())) {
            return;
        }
        userRepository.findByEmail(requestedEmail).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                log.warn("Pokus o změnu e-mailu na již obsazený: userId={}, požadovaný e-mail={}",
                        user.getId(), requestedEmail);
                throw new ConflictException("Uživatel s tímto e-mailem již existuje.");
            }
        });
    }

    private void applyNewPassword(User user, String newPassword) {
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }
    }
}
