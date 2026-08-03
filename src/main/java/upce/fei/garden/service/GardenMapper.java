package upce.fei.garden.service;

import upce.fei.garden.dto.garden.CreateGardenRequest;
import upce.fei.garden.dto.garden.GardenDetailResponse;
import upce.fei.garden.dto.garden.UpdateGardenRequest;
import upce.fei.garden.model.Address;
import upce.fei.garden.model.Garden;
import upce.fei.garden.model.Owner;

/**
 * Převod mezi entitou {@link Garden} a jejími DTO. Držen mimo {@link GardenService},
 * aby servisní vrstva obsahovala pouze business logiku.
 */
final class GardenMapper {

    private GardenMapper() {
    }

    static Garden toEntity(CreateGardenRequest request, Owner owner) {
        Garden garden = new Garden();
        garden.setOwner(owner);
        garden.setGardenName(request.getGardenName());
        garden.setAreaSqm(request.getAreaSqm());
        garden.setAddress(new Address(request.getCity(), request.getStreet(), request.getHouseNumber(),
                request.getPostalCode()));
        return garden;
    }

    static void updateEntity(Garden garden, UpdateGardenRequest request) {
        garden.setGardenName(request.getGardenName());
        garden.setAreaSqm(request.getAreaSqm());
        garden.setAddress(new Address(request.getCity(), request.getStreet(), request.getHouseNumber(),
                request.getPostalCode()));
        // mainPhotoUrl se zde neupravuje - spravuje ho vyhradne FileStorageService pres
        // GardenController#uploadPhoto/deletePhoto, aby fotografii nešlo nastavit na libovolnou
        // nevalidovanou hodnotu a aby ji běžná úprava zahrady nepřepsala na null
    }

    static GardenDetailResponse toResponse(Garden garden) {
        Address address = garden.getAddress();
        return new GardenDetailResponse(
                garden.getId(),
                garden.getGardenName(),
                garden.getAreaSqm(),
                address != null ? address.getCity() : null,
                address != null ? address.getStreet() : null,
                address != null ? address.getHouseNumber() : null,
                address != null ? address.getPostalCode() : null,
                garden.getMainPhotoUrl());
    }
}
