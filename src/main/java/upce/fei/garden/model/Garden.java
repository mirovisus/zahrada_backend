package upce.fei.garden.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Zahrada – profil zahrady vytvořený zákazníkem.
 * Obsahuje popis, lokalitu a fotografie. Ke každé zahradě lze vytvořit poptávky.
 */

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Garden {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    private String gardenName;
    private Double areaSqm; // plocha zahrady
    // TODO: Double lawnAreaSqm
    // TODO: Boolean hasPool
    // TODO: Boolean hasTrees

    @Embedded
    private Address address;

    private String mainPhotoUrl;

    @OneToMany(mappedBy = "garden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Demand> demands = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
}
