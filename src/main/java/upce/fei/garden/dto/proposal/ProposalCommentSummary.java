package upce.fei.garden.dto.proposal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Komentář vlastníka k žádosti o úpravu návrhu.
 */
@Schema(description = "Komentář vlastníka k žádosti o úpravu návrhu")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProposalCommentSummary {
    private String text;
    private LocalDateTime createdAt;
}
