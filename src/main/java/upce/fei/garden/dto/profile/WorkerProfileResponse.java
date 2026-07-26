package upce.fei.garden.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Profil zahradníka – osobní údaje včetně životopisu.
 */
@Schema(description = "Profil zahradníka")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WorkerProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String bio;

    @Schema(description = "Průměrné hodnocení z dokončených zakázek; null, dokud zahradník nemá žádné hodnocení")
    private Double averageRating;
    private String email;
    private String phoneNumber;
    private String avatarUrl; // TODO: implementovat nahrávání souborů
}
