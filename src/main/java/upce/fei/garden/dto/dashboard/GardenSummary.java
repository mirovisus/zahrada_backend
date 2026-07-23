package upce.fei.garden.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Přehledová karta zahrady – název, adresa a hlavní fotografie.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GardenSummary {
    private String gardenName;
    private String city;
    private String street;
    private String houseNumber;
    private String mainPhotoUrl; // TODO: implementovat nahrávání souborů
    // TODO: int activeDemands
}
