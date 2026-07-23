package upce.fei.garden.dto.proposal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Žádost o úpravu návrhu – komentář vlastníka zahrady s požadovanými změnami.
 * */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProposalRequest {
    @NotBlank(message = "Komentář je povinný")
    @Size(min=50, max = 500, message = "Komentář musi obsahovat aspon 50 znaku a nesmi prekrocit 500 znaku")
    private String comment;
}
