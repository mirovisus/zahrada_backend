package upce.fei.garden.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Profil vlastníka zahrady – zobrazení osobních údajů.
 */
@Schema(description = "Profil vlastníka zahrady")
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
