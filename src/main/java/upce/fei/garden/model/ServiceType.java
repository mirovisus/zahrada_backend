package upce.fei.garden.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Typ zahradnické služby – číselník dostupných prací.
 */

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
}
