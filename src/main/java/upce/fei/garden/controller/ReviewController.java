package upce.fei.garden.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import upce.fei.garden.dto.demand.DemandDetailResponse;
import upce.fei.garden.dto.review.CreateReview;
import upce.fei.garden.service.ReviewService;

/**
 * Přijetí dokončené práce vlastníkem a jeho hodnocení zahradníka (viz {@link ReviewService}).
 */
@Tag(name = "Hodnocení", description = "Přijetí dokončené práce vlastníkem spolu s hodnocením zahradníka")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Přijetí dokončené práce a hodnocení zahradníka jednou akcí (pouze ve stavu Práce dokončeny)")
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/api/demands/{id}/accept-work")
    public ResponseEntity<DemandDetailResponse> acceptWork(
            @Parameter(description = "Id poptávky") @PathVariable Long id,
            @Valid @RequestBody CreateReview request) {
        return ResponseEntity.ok(reviewService.acceptWork(id, request));
    }
}
