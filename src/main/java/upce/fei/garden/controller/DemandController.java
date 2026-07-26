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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import upce.fei.garden.dto.demand.CreateDemandRequest;
import upce.fei.garden.dto.demand.DemandDetailResponse;
import upce.fei.garden.dto.demand.DemandStatisticsResponse;
import upce.fei.garden.dto.demand.DemandSummary;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.service.DemandService;

import java.util.List;

/**
 * CRUD nad poptávkami přihlášeného vlastníka a veřejný katalog poptávek pro zahradníky
 * (viz {@link DemandService}).
 */
@Tag(name = "Poptávky", description = "Správa poptávek vlastníka a veřejný katalog pro zahradníky")
@RestController
@RequiredArgsConstructor
public class DemandController {

    private final DemandService demandService;

    @Operation(summary = "Seznam poptávek přihlášeného vlastníka, volitelně filtrovaný podle stavu")
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/api/demands")
    public ResponseEntity<Page<DemandSummary>> getMyDemands(
            @Parameter(description = "Volitelný filtr podle stavu poptávky; bez parametru se vrátí všechny stavy")
            @RequestParam(required = false) DemandStatus status,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(demandService.getMyDemands(pageable, status));
    }

    @Operation(summary = "Seznam poptávek dané zahrady přihlášeného vlastníka")
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/api/gardens/{gardenId}/demands")
    public ResponseEntity<Page<DemandSummary>> getByGarden(
            @Parameter(description = "Id zahrady") @PathVariable Long gardenId,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(demandService.getByGarden(gardenId, pageable));
    }

    @Operation(summary = "Detail poptávky (vlastník vidí jen své, zahradník jen ve stavu NOVA)")
    @PreAuthorize("hasAnyRole('OWNER', 'WORKER')")
    @GetMapping("/api/demands/{id}")
    public ResponseEntity<DemandDetailResponse> getById(
            @Parameter(description = "Id poptávky") @PathVariable Long id) {
        return ResponseEntity.ok(demandService.getById(id));
    }

    @Operation(summary = "Vytvoření nové poptávky pro danou zahradu")
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/api/gardens/{gardenId}/demands")
    public ResponseEntity<DemandDetailResponse> create(
            @Parameter(description = "Id zahrady, pro kterou se poptávka vytváří") @PathVariable Long gardenId,
            @Valid @RequestBody CreateDemandRequest request) {
        DemandDetailResponse response = demandService.create(gardenId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Aktualizace poptávky (nelze, pokud už k ní existuje návrh)")
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/api/demands/{id}")
    public ResponseEntity<DemandDetailResponse> update(
            @Parameter(description = "Id poptávky") @PathVariable Long id,
            @Valid @RequestBody CreateDemandRequest request) {
        return ResponseEntity.ok(demandService.update(id, request));
    }

    @Operation(summary = "Smazání poptávky (nelze, pokud už k ní existuje návrh)")
    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/api/demands/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Id poptávky") @PathVariable Long id) {
        demandService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Veřejný katalog poptávek pro zahradníky (filtr podle města, typů služeb a fulltextové hledání)")
    @GetMapping("/api/demands/catalog")
    public ResponseEntity<Page<DemandSummary>> getCatalog(
            @Parameter(description = "Filtr podle města zahrady (přesná shoda, bez ohledu na velikost písmen)")
            @RequestParam(required = false) String city,
            @Parameter(description = "Filtr podle id typů služeb - poptávka musí obsahovat alespoň jeden z uvedených")
            @RequestParam(required = false) List<Long> serviceTypeIds,
            @Parameter(description = "Fulltextové hledání v názvu a popisu poptávky")
            @RequestParam(required = false) String search,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(demandService.getCatalog(city, serviceTypeIds, search, pageable));
    }

    @Operation(summary = "Přehled poptávek vlastníka s počtem přijatých návrhů")
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/api/demands/statistics")
    public ResponseEntity<List<DemandStatisticsResponse>> getStatistics() {
        return ResponseEntity.ok(demandService.getMyDemandsWithProposalCount());
    }
}
