package upce.fei.garden.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import upce.fei.garden.dto.servicetype.ServiceTypeResponse;
import upce.fei.garden.service.ServiceTypeService;

import java.util.List;

/**
 * Číselník typů zahradnických služeb, dostupný všem přihlášeným uživatelům.
 */
@Tag(name = "Typy služeb", description = "Číselník dostupných zahradnických služeb")
@RestController
@RequestMapping("/api/service-types")
@RequiredArgsConstructor
public class ServiceTypeController {

    private final ServiceTypeService serviceTypeService;

    @Operation(summary = "Seznam všech typů služeb")
    @GetMapping
    public ResponseEntity<List<ServiceTypeResponse>> getAll() {
        return ResponseEntity.ok(serviceTypeService.getAll());
    }
}
