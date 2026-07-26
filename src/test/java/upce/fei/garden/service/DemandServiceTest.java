package upce.fei.garden.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import upce.fei.garden.dto.demand.CreateDemandRequest;
import upce.fei.garden.dto.demand.DemandDetailResponse;
import upce.fei.garden.exception.ConflictException;
import upce.fei.garden.exception.NotFoundException;
import upce.fei.garden.exception.ValidationException;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.Garden;
import upce.fei.garden.model.Owner;
import upce.fei.garden.model.ServiceType;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.repository.DemandRepository;
import upce.fei.garden.repository.GardenRepository;
import upce.fei.garden.repository.ProposalRepository;
import upce.fei.garden.repository.ServiceTypeRepository;
import upce.fei.garden.security.CurrentUserService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit testy {@link DemandService} bez Spring kontextu – repozitáře a {@link CurrentUserService}
 * jsou mockované přes Mockito, testuje se jen business logika samotné třídy.
 */
@ExtendWith(MockitoExtension.class)
class DemandServiceTest {

    @Mock
    private DemandRepository demandRepository;

    @Mock
    private GardenRepository gardenRepository;

    @Mock
    private ServiceTypeRepository serviceTypeRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private DemandService demandService;

    private Owner owner;
    private Garden garden;
    private ServiceType serviceType;

    @BeforeEach
    void setUp() {
        owner = new Owner();
        owner.setId(1L);

        garden = new Garden();
        garden.setId(10L);
        garden.setOwner(owner);
        garden.setGardenName("Zahrada za domem");

        serviceType = new ServiceType();
        serviceType.setId(5L);
        serviceType.setName("Sekání trávníku");
    }

    private CreateDemandRequest validRequest() {
        return new CreateDemandRequest("Posekat trávník", List.of(5L), "Popis", LocalDate.now().plusDays(1));
    }

    // --- create ---

    @Test
    void create_success() {
        when(currentUserService.getCurrentOwner()).thenReturn(owner);
        when(gardenRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(garden));
        when(serviceTypeRepository.findAllByIdIn(List.of(5L))).thenReturn(List.of(serviceType));
        when(demandRepository.save(any(Demand.class))).thenAnswer(invocation -> {
            Demand demand = invocation.getArgument(0);
            demand.setId(100L);
            return demand;
        });

        DemandDetailResponse response = demandService.create(10L, validRequest());

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("Posekat trávník");
        assertThat(response.getStatus()).isEqualTo(DemandStatus.NOVA);
        assertThat(response.getServiceTypeNames()).containsExactly("Sekání trávníku");
        assertThat(response.isHasProposals()).isFalse();
    }

    @Test
    void create_unknownServiceType_throwsValidationException() {
        when(currentUserService.getCurrentOwner()).thenReturn(owner);
        when(gardenRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(garden));
        // pozadovana sluzba id=5, ale v cisleniku existuje jen jina
        when(serviceTypeRepository.findAllByIdIn(List.of(5L))).thenReturn(List.of());

        assertThatThrownBy(() -> demandService.create(10L, validRequest()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("5");

        verify(demandRepository, never()).save(any());
    }

    @Test
    void create_foreignGarden_throwsNotFoundException() {
        when(currentUserService.getCurrentOwner()).thenReturn(owner);
        when(gardenRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> demandService.create(10L, validRequest()))
                .isInstanceOf(NotFoundException.class);

        verify(serviceTypeRepository, never()).findAllByIdIn(any());
        verify(demandRepository, never()).save(any());
    }

    // --- update ---

    @Test
    void update_success() {
        Demand existing = new Demand();
        existing.setId(50L);
        existing.setGarden(garden);
        existing.setTitle("Puvodni nazev");
        existing.setStatus(DemandStatus.NOVA);

        when(currentUserService.getCurrentOwner()).thenReturn(owner);
        when(demandRepository.findByIdAndGardenOwnerId(50L, 1L)).thenReturn(Optional.of(existing));
        when(proposalRepository.existsByDemandId(50L)).thenReturn(false);
        when(serviceTypeRepository.findAllByIdIn(List.of(5L))).thenReturn(List.of(serviceType));
        when(demandRepository.save(any(Demand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateDemandRequest updateRequest =
                new CreateDemandRequest("Novy nazev", List.of(5L), "Novy popis", LocalDate.now().plusDays(2));

        DemandDetailResponse response = demandService.update(50L, updateRequest);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getTitle()).isEqualTo("Novy nazev");
        assertThat(response.getServiceTypeNames()).containsExactly("Sekání trávníku");
    }

    @Test
    void update_withExistingProposals_throwsConflictException() {
        Demand existing = new Demand();
        existing.setId(50L);
        existing.setGarden(garden);
        existing.setStatus(DemandStatus.NOVA);

        when(currentUserService.getCurrentOwner()).thenReturn(owner);
        when(demandRepository.findByIdAndGardenOwnerId(50L, 1L)).thenReturn(Optional.of(existing));
        when(proposalRepository.existsByDemandId(50L)).thenReturn(true);

        assertThatThrownBy(() -> demandService.update(50L, validRequest()))
                .isInstanceOf(ConflictException.class);

        verify(serviceTypeRepository, never()).findAllByIdIn(any());
        verify(demandRepository, never()).save(any());
    }

    // --- delete ---

    @Test
    void delete_withExistingProposals_throwsConflictException() {
        Demand existing = new Demand();
        existing.setId(50L);
        existing.setGarden(garden);

        when(currentUserService.getCurrentOwner()).thenReturn(owner);
        when(demandRepository.findByIdAndGardenOwnerId(50L, 1L)).thenReturn(Optional.of(existing));
        when(proposalRepository.existsByDemandId(50L)).thenReturn(true);

        assertThatThrownBy(() -> demandService.delete(50L))
                .isInstanceOf(ConflictException.class);

        verify(demandRepository, never()).delete(any(Demand.class));
    }

    @Test
    void delete_foreignDemand_throwsNotFoundException() {
        when(currentUserService.getCurrentOwner()).thenReturn(owner);
        when(demandRepository.findByIdAndGardenOwnerId(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> demandService.delete(999L))
                .isInstanceOf(NotFoundException.class);

        verify(proposalRepository, never()).existsByDemandId(any());
        verify(demandRepository, never()).delete(any(Demand.class));
    }
}
