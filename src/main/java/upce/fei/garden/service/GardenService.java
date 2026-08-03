package upce.fei.garden.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import upce.fei.garden.dto.garden.CreateGardenRequest;
import upce.fei.garden.dto.garden.GardenDetailResponse;
import upce.fei.garden.dto.garden.UpdateGardenRequest;
import upce.fei.garden.exception.NotFoundException;
import upce.fei.garden.model.Garden;
import upce.fei.garden.model.Owner;
import upce.fei.garden.repository.GardenRepository;
import upce.fei.garden.security.CurrentUserService;

/**
 * Správa zahrad přihlášeného vlastníka (CRUD). Vlastnictví zahrady se vždy odvozuje od
 * aktuálně přihlášeného uživatele přes {@link CurrentUserService#getCurrentOwner()} – vlastník
 * tedy nikdy nemůže přistoupit k zahradě jiného vlastníka, a to ani na čtení.
 * <p>
 * Přístup k cizí zahradě je při dotazu podle id vždy hlášen jako {@link NotFoundException} (HTTP 404),
 * nikoliv jako zákaz přístupu (HTTP 403) – nechceme cizímu uživateli prozrazovat, že daný záznam vůbec existuje.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GardenService {

    private final GardenRepository gardenRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;

    /**
     * Vrátí stránkovaný seznam zahrad aktuálně přihlášeného vlastníka.
     */
    @Transactional(readOnly = true)
    public Page<GardenDetailResponse> getMyGardens(Pageable pageable) {
        Owner owner = currentUserService.getCurrentOwner();
        return gardenRepository.findAllByOwnerId(owner.getId(), pageable)
                .map(GardenMapper::toResponse);
    }

    /**
     * Vrátí detail zahrady podle id, pokud patří aktuálně přihlášenému vlastníkovi.
     *
     * @throws NotFoundException pokud zahrada neexistuje nebo nepatří přihlášenému vlastníkovi
     */
    @Transactional(readOnly = true)
    public GardenDetailResponse getById(Long id) {
        Owner owner = currentUserService.getCurrentOwner();
        Garden garden = findOwnedGarden(id, owner);
        return GardenMapper.toResponse(garden);
    }

    /**
     * Vytvoří novou zahradu pro aktuálně přihlášeného vlastníka.
     */
    @Transactional
    public GardenDetailResponse create(CreateGardenRequest request) {
        Owner owner = currentUserService.getCurrentOwner();
        Garden garden = GardenMapper.toEntity(request, owner);

        Garden saved = gardenRepository.save(garden);
        log.info("Vytvořena zahrada: id={}, ownerId={}", saved.getId(), owner.getId());

        return GardenMapper.toResponse(saved);
    }

    /**
     * Aktualizuje zahradu, pokud patří aktuálně přihlášenému vlastníkovi.
     *
     * @throws NotFoundException pokud zahrada neexistuje nebo nepatří přihlášenému vlastníkovi
     */
    @Transactional
    public GardenDetailResponse update(Long id, UpdateGardenRequest request) {
        Owner owner = currentUserService.getCurrentOwner();
        Garden garden = findOwnedGarden(id, owner);

        GardenMapper.updateEntity(garden, request);

        Garden saved = gardenRepository.save(garden);
        log.info("Aktualizována zahrada: id={}, ownerId={}", saved.getId(), owner.getId());

        return GardenMapper.toResponse(saved);
    }

    /**
     * Smaže zahradu, pokud patří aktuálně přihlášenému vlastníkovi. Spolu se zahradou smaže
     * i soubor její fotografie (pokud existuje), jinak by v {@code app.upload.dir} zůstal osiřelý.
     *
     * @throws NotFoundException pokud zahrada neexistuje nebo nepatří přihlášenému vlastníkovi
     */
    @Transactional
    public void delete(Long id) {
        Owner owner = currentUserService.getCurrentOwner();
        Garden garden = findOwnedGarden(id, owner);

        gardenRepository.delete(garden);
        fileStorageService.delete(garden.getMainPhotoUrl());
        log.info("Smazána zahrada: id={}, ownerId={}", id, owner.getId());
    }

    /**
     * Nahraje (nebo nahradí) fotografii zahrady. Validace a sanitizace nahraného souboru
     * (velikost, typ, skutečná signatura obsahu) je v {@link FileStorageService#store}. Starý
     * soubor fotografie se smaže až po úspěšném uložení nového a zápisu do databáze, aby v případě
     * chyby validace nepřišla zahrada o dosavadní fotografii.
     *
     * @throws NotFoundException   pokud zahrada neexistuje nebo nepatří přihlášenému vlastníkovi
     * @throws upce.fei.garden.exception.ValidationException pokud nahraný soubor nesplní validaci
     */
    @Transactional
    public GardenDetailResponse uploadPhoto(Long id, MultipartFile file) {
        Owner owner = currentUserService.getCurrentOwner();
        Garden garden = findOwnedGarden(id, owner);

        String oldPhotoUrl = garden.getMainPhotoUrl();
        String newPhotoUrl = fileStorageService.store(file);
        garden.setMainPhotoUrl(newPhotoUrl);
        Garden saved = gardenRepository.save(garden);

        fileStorageService.delete(oldPhotoUrl);
        log.info("Nahrána fotografie zahrady: id={}, ownerId={}", saved.getId(), owner.getId());

        return GardenMapper.toResponse(saved);
    }

    /**
     * Smaže fotografii zahrady (soubor i odkaz {@code mainPhotoUrl}), pokud zahrada patří
     * aktuálně přihlášenému vlastníkovi. Pokud zahrada žádnou fotografii nemá, jde o no-op.
     *
     * @throws NotFoundException pokud zahrada neexistuje nebo nepatří přihlášenému vlastníkovi
     */
    @Transactional
    public GardenDetailResponse deletePhoto(Long id) {
        Owner owner = currentUserService.getCurrentOwner();
        Garden garden = findOwnedGarden(id, owner);

        fileStorageService.delete(garden.getMainPhotoUrl());
        garden.setMainPhotoUrl(null);
        Garden saved = gardenRepository.save(garden);
        log.info("Smazána fotografie zahrady: id={}, ownerId={}", saved.getId(), owner.getId());

        return GardenMapper.toResponse(saved);
    }

    private Garden findOwnedGarden(Long id, Owner owner) {
        return gardenRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> {
                    log.warn("Pokus o přístup k cizí nebo neexistující zahradě: id={}, ownerId={}", id, owner.getId());
                    return new NotFoundException("Zahrada s id " + id + " nebyla nalezena.");
                });
    }
}
