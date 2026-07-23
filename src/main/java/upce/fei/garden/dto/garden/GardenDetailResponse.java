package upce.fei.garden.dto.garden;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Detail zahrady – všechna data pro zobrazení a úpravu formuláře.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GardenDetailResponse {
    private Long id;
    private String gardenName;
    private Double areaSqm; // plocha zahrady
    // TODO: Double lawnAreaSqm
    // TODO: Boolean hasPool
    // TODO: Boolean hasTrees
    private String city;
    private String street;
    private String houseNumber;
    private String postalCode;
    private String mainPhotoUrl; // TODO: implementovat nahrávání souborů
}
