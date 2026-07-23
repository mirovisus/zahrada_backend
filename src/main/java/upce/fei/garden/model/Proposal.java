package upce.fei.garden.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import upce.fei.garden.model.enums.ProposalStatus;

/**
 * Návrh – nabídka zahradníka na realizaci poptávky.
 * Zákazník může návrh schválit, zamítnout nebo požádat o úpravy.
 */

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Proposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id", nullable = false)
    private Demand demand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    private String description;
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private ProposalStatus status = ProposalStatus.NOVY;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalComment> comments = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
