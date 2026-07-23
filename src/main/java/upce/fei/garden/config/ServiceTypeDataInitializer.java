package upce.fei.garden.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import upce.fei.garden.model.ServiceType;
import upce.fei.garden.repository.ServiceTypeRepository;

import java.util.List;

/**
 * Naplní číselník typů zahradnických služeb výchozími hodnotami, pokud je tabulka prázdná
 * (např. při prvním spuštění aplikace na čisté databázi).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceTypeDataInitializer implements CommandLineRunner {

    private static final List<String> DEFAULT_SERVICE_TYPES = List.of(
            "Sekání trávníku",
            "Výsadba keřů",
            "Údržba zahrady",
            "Instalace zavlažování",
            "Návrh zahrady",
            "Kácení",
            "Plotování",
            "Odstraňování pařezů"
    );

    private final ServiceTypeRepository serviceTypeRepository;

    @Override
    public void run(String... args) {
        if (serviceTypeRepository.count() > 0) {
            return;
        }

        List<ServiceType> serviceTypes = DEFAULT_SERVICE_TYPES.stream()
                .map(name -> new ServiceType(null, name))
                .toList();
        serviceTypeRepository.saveAll(serviceTypes);
        log.info("Naplněn číselník typů služeb: {} záznamů", serviceTypes.size());
    }
}
