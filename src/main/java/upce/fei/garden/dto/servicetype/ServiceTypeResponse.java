package upce.fei.garden.dto.servicetype;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Položka číselníku typů zahradnických služeb.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTypeResponse {
    private Long id;
    private String name;
}
