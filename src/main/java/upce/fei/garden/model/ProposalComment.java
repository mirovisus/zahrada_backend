package upce.fei.garden.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Komentář k návrhu – zpětná vazba zákazníka při žádosti o úpravy návrhu.
 */

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProposalComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    private String text;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
