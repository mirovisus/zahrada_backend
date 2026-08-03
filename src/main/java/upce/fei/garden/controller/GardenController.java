package upce.fei.garden.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import upce.fei.garden.dto.garden.CreateGardenRequest;
import upce.fei.garden.dto.garden.GardenDetailResponse;
import upce.fei.garden.dto.garden.UpdateGardenRequest;
import upce.fei.garden.service.GardenService;

/**
 * CRUD nad zahradami přihlášeného vlastníka. Všechny endpointy jsou dostupné pouze roli
 * {@code OWNER} a pracují výhradně se zahradami aktuálně přihlášeného vlastníka (viz {@link GardenService}).
 */
@Tag(name = "Zahrady", description = "Správa zahrad vlastníka")
@RestController
@RequestMapping("/api/gardens")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class GardenController {

    private final GardenService gardenService;

    @Operation(summary = "Seznam zahrad přihlášeného vlastníka (stránkovaně)")
    @GetMapping
    public ResponseEntity<Page<GardenDetailResponse>> getMyGardens(
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(gardenService.getMyGardens(pageable));
    }

    @Operation(summary = "Detail zahrady podle id")
    @GetMapping("/{id}")
    public ResponseEntity<GardenDetailResponse> getById(
            @Parameter(description = "Id zahrady") @PathVariable Long id) {
        return ResponseEntity.ok(gardenService.getById(id));
    }

    @Operation(summary = "Vytvoření nové zahrady")
    @PostMapping
    public ResponseEntity<GardenDetailResponse> create(@Valid @RequestBody CreateGardenRequest request) {
        GardenDetailResponse response = gardenService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Aktualizace zahrady")
    @PutMapping("/{id}")
    public ResponseEntity<GardenDetailResponse> update(
            @Parameter(description = "Id zahrady") @PathVariable Long id,
            @Valid @RequestBody UpdateGardenRequest request) {
        return ResponseEntity.ok(gardenService.update(id, request));
    }

    @Operation(summary = "Smazání zahrady")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Id zahrady") @PathVariable Long id) {
        gardenService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Nahrání fotografie zahrady (nahradí předchozí, pokud existuje)")
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GardenDetailResponse> uploadPhoto(
            @Parameter(description = "Id zahrady") @PathVariable Long id,
            @Parameter(description = "Soubor fotografie (JPEG/PNG/WebP)") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(gardenService.uploadPhoto(id, file));
    }

    @Operation(summary = "Smazání fotografie zahrady")
    @DeleteMapping("/{id}/photo")
    public ResponseEntity<GardenDetailResponse> deletePhoto(
            @Parameter(description = "Id zahrady") @PathVariable Long id) {
        return ResponseEntity.ok(gardenService.deletePhoto(id));
    }
}
