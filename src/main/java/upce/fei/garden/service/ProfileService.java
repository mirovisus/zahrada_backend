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
import upce.fei.garden.model.Owner;
import upce.fei.garden.model.User;
import upce.fei.garden.model.Worker;
import upce.fei.garden.repository.UserRepository;
import upce.fei.garden.security.CurrentUserService;

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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
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
