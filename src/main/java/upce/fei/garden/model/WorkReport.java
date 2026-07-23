package upce.fei.garden.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Report prací – záznam o dokončených pracích vytvořený zahradníkem.
 * Obsahuje popis provedených úkonů a fotografie výsledku.
 */

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id", unique = true)
    private Demand demand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    private String description;

    @ElementCollection
    private List<String> photoUrls = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
}
