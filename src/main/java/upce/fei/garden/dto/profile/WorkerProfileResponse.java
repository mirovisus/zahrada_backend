package upce.fei.garden.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Profil zahradníka – osobní údaje včetně životopisu.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WorkerProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String bio;
    private Double averageRating;
    private String email;
    private String phoneNumber;
    private String avatarUrl; // TODO: implementovat nahrávání souborů
}
