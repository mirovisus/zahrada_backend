package upce.fei.garden.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import upce.fei.garden.model.enums.DemandStatus;

/**
 * Poptávka – požadavek zákazníka na úpravu zahrady.
 * Prochází životním cyklem: NOVA → SCHVALENA → ZAPLACENA → PRACE_DOKONCENY → PRACE_SCHVALENY (BPMN 04–06).
 */

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Demand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garden_id", nullable = false)
    private Garden garden;

    @ManyToMany
    @JoinTable(
            name = "demand_service_type",
            joinColumns = @JoinColumn(name = "demand_id"),
            inverseJoinColumns = @JoinColumn(name = "service_type_id")
    )
    private List<ServiceType> serviceTypes = new ArrayList<>();

    private String description;

    private LocalDate desiredDate;

    @Enumerated(EnumType.STRING)
    private DemandStatus status = DemandStatus.NOVA;

    @OneToMany(mappedBy = "demand", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Proposal> proposals = new ArrayList<>();

    @OneToOne(mappedBy = "demand")
    private WorkReport workReport;

    @OneToOne(mappedBy = "demand")
    private Review review;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
