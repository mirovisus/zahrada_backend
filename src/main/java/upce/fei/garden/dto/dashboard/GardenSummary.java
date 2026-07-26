package upce.fei.garden.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Přehledová karta zahrady – název, adresa a hlavní fotografie.
 */
@Schema(description = "Přehledová karta zahrady - používá se jako vnořený objekt v detailu poptávky")
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
