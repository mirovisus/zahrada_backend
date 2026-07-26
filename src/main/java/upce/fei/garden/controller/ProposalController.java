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
import upce.fei.garden.dto.proposal.CreateProposalRequest;
import upce.fei.garden.dto.proposal.ProposalSummary;
import upce.fei.garden.service.ProposalService;

import java.util.List;

/**
 * Návrhy (nabídky) zahradníků na poptávky a jejich schvalování vlastníkem (viz {@link ProposalService}).
 */
@Tag(name = "Návrhy", description = "Podávání a schvalování návrhů zahradníků na poptávky")
@RestController
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    @Operation(summary = "Podání nového návrhu zahradníka na poptávku")
    @PreAuthorize("hasRole('WORKER')")
    @PostMapping("/api/demands/{demandId}/proposals")
    public ResponseEntity<ProposalSummary> create(
            @Parameter(description = "Id poptávky, na kterou se návrh podává") @PathVariable Long demandId,
            @Valid @RequestBody CreateProposalRequest request) {
        ProposalSummary response = proposalService.create(demandId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Seznam návrhů dané poptávky (pouze vlastník poptávky)")
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/api/demands/{demandId}/proposals")
    public ResponseEntity<List<ProposalSummary>> getByDemand(
            @Parameter(description = "Id poptávky") @PathVariable Long demandId) {
        return ResponseEntity.ok(proposalService.getByDemand(demandId));
    }

    @Operation(summary = "Seznam návrhů podaných přihlášeným zahradníkem")
    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/api/proposals/my")
    public ResponseEntity<Page<ProposalSummary>> getMyProposals(
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(proposalService.getMyProposals(pageable));
    }

    @Operation(summary = "Přijetí návrhu (ostatní návrhy poptávky jsou zamítnuty, poptávka je schválena)")
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/api/proposals/{id}/accept")
    public ResponseEntity<ProposalSummary> accept(@Parameter(description = "Id návrhu") @PathVariable Long id) {
        return ResponseEntity.ok(proposalService.accept(id));
    }

    @Operation(summary = "Zamítnutí návrhu")
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/api/proposals/{id}/reject")
    public ResponseEntity<ProposalSummary> reject(@Parameter(description = "Id návrhu") @PathVariable Long id) {
        return ResponseEntity.ok(proposalService.reject(id));
    }

    @Operation(summary = "Odvolání vlastního návrhu zahradníkem (pouze ve stavu Nový)")
    @PreAuthorize("hasRole('WORKER')")
    @DeleteMapping("/api/proposals/{id}")
    public ResponseEntity<Void> withdraw(@Parameter(description = "Id návrhu") @PathVariable Long id) {
        proposalService.withdraw(id);
        return ResponseEntity.noContent().build();
    }
}
