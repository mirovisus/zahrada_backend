package upce.fei.garden.service;

import upce.fei.garden.dto.dashboard.GardenSummary;
import upce.fei.garden.dto.demand.CreateDemandRequest;
import upce.fei.garden.dto.demand.DemandDetailResponse;
import upce.fei.garden.dto.demand.DemandStatisticsResponse;
import upce.fei.garden.dto.demand.DemandSummary;
import upce.fei.garden.model.Address;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.Garden;
import upce.fei.garden.model.ServiceType;
import upce.fei.garden.model.enums.DemandStatus;
import upce.fei.garden.repository.DemandWithProposalCount;

import java.util.List;

/**
 * Převod mezi entitou {@link Demand} a jejími DTO. Držen mimo {@link DemandService},
 * aby servisní vrstva obsahovala pouze business logiku.
 */
final class DemandMapper {

    private static final int DESCRIPTION_PREVIEW_LENGTH = 100;

    private DemandMapper() {
    }

    static Demand toEntity(CreateDemandRequest request, Garden garden, List<ServiceType> serviceTypes) {
        Demand demand = new Demand();
        demand.setGarden(garden);
        demand.setTitle(request.getTitle());
        demand.setDescription(request.getDescription());
        demand.setDesiredDate(request.getDesiredDate());
        demand.setServiceTypes(serviceTypes);
        demand.setStatus(DemandStatus.NOVA);
        return demand;
    }

    static void updateEntity(Demand demand, CreateDemandRequest request, List<ServiceType> serviceTypes) {
        demand.setTitle(request.getTitle());
        demand.setDescription(request.getDescription());
        demand.setDesiredDate(request.getDesiredDate());
        demand.setServiceTypes(serviceTypes);
    }

    static DemandSummary toSummary(Demand demand) {
        Garden garden = demand.getGarden();
        return new DemandSummary(
                demand.getId(),
                garden != null ? garden.getGardenName() : null,
                previewOf(demand.getDescription()),
                demand.getDesiredDate(),
                demand.getStatus());
    }

    static DemandDetailResponse toDetailResponse(Demand demand, boolean hasProposals) {
        return new DemandDetailResponse(
                demand.getId(),
                demand.getTitle(),
                demand.getDescription(),
                demand.getDesiredDate(),
                demand.getCreatedAt(),
                demand.getStatus(),
                demand.getServiceTypes().stream().map(ServiceType::getName).toList(),
                toGardenSummary(demand.getGarden()),
                hasProposals);
    }

    static DemandStatisticsResponse toStatistics(DemandWithProposalCount projection) {
        return new DemandStatisticsResponse(
                projection.getDemandId(),
                projection.getTitle(),
                projection.getStatus(),
                projection.getProposalCount());
    }

    private static GardenSummary toGardenSummary(Garden garden) {
        if (garden == null) {
            return null;
        }
        Address address = garden.getAddress();
        return new GardenSummary(
                garden.getGardenName(),
                address != null ? address.getCity() : null,
                address != null ? address.getStreet() : null,
                address != null ? address.getHouseNumber() : null,
                garden.getMainPhotoUrl());
    }

    private static String previewOf(String description) {
        if (description == null || description.length() <= DESCRIPTION_PREVIEW_LENGTH) {
            return description;
        }
        return description.substring(0, DESCRIPTION_PREVIEW_LENGTH) + "...";
    }
}
