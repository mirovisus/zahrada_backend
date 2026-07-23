package upce.fei.garden.dto.workreport;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Vytvoření zprávy o provedených pracích zahradníkem po dokončení zakázky.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkReport {
    @NotBlank(message = "Popis prací je povinný")
    @Size(min = 50, max = 2000, message = "Popis musí obsahovat alespoň 50 znaků a nesmí překročit 2000 znaků")
    private String description;

    // TODO: implementovat nahrávání fotek
    private List<String> photoUrls;
}
