package upce.fei.garden.dto.servicetype;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Položka číselníku typů zahradnických služeb.
 */
@Schema(description = "Položka číselníku typů zahradnických služeb")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTypeResponse {
    private Long id;
    private String name;
}
