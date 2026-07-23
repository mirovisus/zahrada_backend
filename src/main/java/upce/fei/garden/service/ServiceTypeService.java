package upce.fei.garden.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upce.fei.garden.dto.servicetype.ServiceTypeResponse;
import upce.fei.garden.repository.ServiceTypeRepository;

import java.util.List;

/**
 * Čtení číselníku typů zahradnických služeb.
 */
@Service
@RequiredArgsConstructor
public class ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;

    /**
     * Vrátí kompletní číselník typů služeb.
     */
    @Transactional(readOnly = true)
    public List<ServiceTypeResponse> getAll() {
        return serviceTypeRepository.findAll().stream()
                .map(serviceType -> new ServiceTypeResponse(serviceType.getId(), serviceType.getName()))
                .toList();
    }
}
