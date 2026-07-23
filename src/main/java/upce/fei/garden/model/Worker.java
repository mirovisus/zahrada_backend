package upce.fei.garden.model;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;

/**
 * Zahradník – pracovník provádějící údržbu zahrad.
 * Odesílá návrhy na poptávky, realizuje práce a získává hodnocení.
 */

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Worker extends User {
    private Double averageRating;
    private String bio;

    @OneToMany(mappedBy = "worker")
    private List<Proposal> proposals = new ArrayList<>();

    @OneToMany(mappedBy = "worker")
    private List<WorkReport> workReports = new ArrayList<>();

    @OneToMany(mappedBy = "worker")
    private List<Review> reviews = new ArrayList<>();
}
