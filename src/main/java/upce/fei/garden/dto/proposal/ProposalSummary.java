package upce.fei.garden.dto.proposal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import upce.fei.garden.model.enums.ProposalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Přehledová karta návrhu – informace o zahradníkovi, cena a stav návrhu.
 */
@Schema(description = "Přehledová karta návrhu zahradníka na poptávku")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProposalSummary {
    private Long id;
    private Long demandId;
    private String workerFirstName;
    private String workerLastName;
    private String workerAvatarUrl;
    private String workerBio;
    private String description;
    private BigDecimal price;
    private ProposalStatus status;
    private LocalDateTime createdAt;
    // TODO: Double workerRating
    // TODO: int completedOrders
}
