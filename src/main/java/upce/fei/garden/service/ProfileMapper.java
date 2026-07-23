package upce.fei.garden.service;

import upce.fei.garden.dto.profile.OwnerProfileResponse;
import upce.fei.garden.dto.profile.WorkerProfileResponse;
import upce.fei.garden.model.Owner;
import upce.fei.garden.model.Worker;

/**
 * Převod mezi entitami {@link Owner}/{@link Worker} a jejich profilovými DTO. Držen mimo
 * {@link ProfileService}, aby servisní vrstva obsahovala pouze business logiku.
 */
final class ProfileMapper {

    private ProfileMapper() {
    }

    static OwnerProfileResponse toResponse(Owner owner) {
        return new OwnerProfileResponse(
                owner.getId(),
                owner.getFirstName(),
                owner.getLastName(),
                owner.getEmail(),
                owner.getPhoneNumber(),
                owner.getAvatarUrl());
    }

    static WorkerProfileResponse toResponse(Worker worker) {
        return new WorkerProfileResponse(
                worker.getId(),
                worker.getFirstName(),
                worker.getLastName(),
                worker.getBio(),
                worker.getAverageRating(),
                worker.getEmail(),
                worker.getPhoneNumber(),
                worker.getAvatarUrl());
    }
}
