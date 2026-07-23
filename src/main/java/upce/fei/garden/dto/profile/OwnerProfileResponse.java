package upce.fei.garden.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Profil vlastníka zahrady – zobrazení osobních údajů.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OwnerProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String avatarUrl; // TODO: implementovat nahrávání souborů
}
