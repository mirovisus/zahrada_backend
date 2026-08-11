package upce.fei.garden.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import upce.fei.garden.dto.workreport.CreateWorkReport;
import upce.fei.garden.dto.workreport.WorkReportResponse;
import upce.fei.garden.dto.workreport.WorkerJobSummary;
import upce.fei.garden.service.WorkReportService;

import java.util.List;

/**
 * Kabinet zahradníka pro realizaci zakázek a odeslání reportu o dokončení prací
 * (viz {@link WorkReportService}).
 */
@Tag(name = "Realizace zakázek", description = "Přehled zakázek zahradníka a odeslání reportu o dokončených pracích")
@RestController
@RequiredArgsConstructor
public class WorkReportController {

    private final WorkReportService workReportService;

    @Operation(summary = "Zakázky přihlášeného zahradníka - poptávky s jeho přijatým návrhem ve stavu Zaplaceno a dále")
    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/api/worker/jobs")
    public ResponseEntity<List<WorkerJobSummary>> getMyJobs() {
        return ResponseEntity.ok(workReportService.getMyJobs());
    }

    @Operation(summary = "Odeslání reportu o dokončených pracích (pouze ve stavu Zaplaceno, poptávka poté přejde do stavu Práce dokončeny)")
    @PreAuthorize("hasRole('WORKER')")
    @PostMapping("/api/demands/{id}/work-report")
    public ResponseEntity<WorkReportResponse> submitReport(
            @Parameter(description = "Id poptávky") @PathVariable Long id,
            @Valid @RequestBody CreateWorkReport request) {
        WorkReportResponse response = workReportService.submitReport(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
