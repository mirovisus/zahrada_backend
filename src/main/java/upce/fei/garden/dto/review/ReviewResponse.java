package upce.fei.garden.dto.review;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Hodnocení zahradníka vlastníkem, vystavené při přijetí dokončené práce.
 */
@Schema(description = "Hodnocení zahradníka vystavené vlastníkem při přijetí práce")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
