package upce.fei.garden.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import upce.fei.garden.dto.profile.OwnerProfileResponse;
import upce.fei.garden.dto.profile.UpdateOwnerProfileRequest;
import upce.fei.garden.dto.profile.UpdateWorkerProfileRequest;
import upce.fei.garden.dto.profile.WorkerProfileResponse;
import upce.fei.garden.service.ProfileService;

/**
 * Profil přihlášeného uživatele. Čtení je dostupné oběma rolím, aktualizace profilu se dělí
 * podle role, protože vlastník a zahradník mají odlišná pole (viz {@link ProfileService}).
 */
@Tag(name = "Profil", description = "Profil přihlášeného uživatele")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "Profil aktuálně přihlášeného uživatele (vlastník nebo zahradník)")
    @GetMapping
    public ResponseEntity<Object> getMyProfile() {
        return ResponseEntity.ok(profileService.getMyProfile());
    }

    @Operation(summary = "Aktualizace profilu vlastníka zahrady")
    @PutMapping("/owner")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OwnerProfileResponse> updateOwnerProfile(
            @Valid @RequestBody UpdateOwnerProfileRequest request) {
        return ResponseEntity.ok(profileService.updateOwnerProfile(request));
    }

    @Operation(summary = "Aktualizace profilu zahradníka")
    @PutMapping("/worker")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<WorkerProfileResponse> updateWorkerProfile(
            @Valid @RequestBody UpdateWorkerProfileRequest request) {
        return ResponseEntity.ok(profileService.updateWorkerProfile(request));
    }

    @Operation(summary = "Smazání vlastního účtu (nevratné; nelze při rozpracované zakázce)")
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyAccount() {
        profileService.deleteMyAccount();
        return ResponseEntity.noContent().build();
    }
}
