package upce.fei.garden.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Hodnocení – hodnocení práce zahradníka zákazníkem (1–5 hvězdiček).
 * Po uložení se aktualizuje průměrné hodnocení zahradníka.
 */

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id", unique = true)
    private Demand demand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private Worker worker;

    @Column(nullable = false)
    private Integer rating;

    private String comment;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
