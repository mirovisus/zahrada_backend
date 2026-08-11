package upce.fei.garden.dto.proposal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Žádost o úpravu návrhu – komentář vlastníka zahrady s požadovanými změnami.
 */
@Schema(description = "Žádost vlastníka o úpravu návrhu - návrh přejde do stavu UPRAVY_POZADOVANY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestChangesRequest {
    @Schema(description = "Komentář s popisem požadovaných úprav")
    @NotBlank(message = "Komentář je povinný")
    @Size(max = 1000, message = "Komentář nesmí překročit 1000 znaků")
    private String comment;
}
