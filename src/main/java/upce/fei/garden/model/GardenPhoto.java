package upce.fei.garden.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Fotografie zahrady – obrázek přiložený k profilu zahrady.
 */

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GardenPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garden_id", nullable = false)
    private Garden garden;

    private String url;
    private String caption;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
