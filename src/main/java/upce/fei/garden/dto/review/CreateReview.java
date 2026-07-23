package upce.fei.garden.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vytvoření hodnocení zahradníka vlastníkem zahrady po dokončení prací.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateReview {
    @NotNull(message = "Hodnocení je povinné")
    @Min(value = 1, message = "Hodnocení musí být alespoň 1")
    @Max(value = 5, message = "Hodnocení nesmí překročit 5")
    private Integer rating;

    @Size(min=4, max = 1000, message = "Komentář musi obsahovat aspon 4 znaky a nesmi prekrocit 1000 znaku")
    private String comment;
}
