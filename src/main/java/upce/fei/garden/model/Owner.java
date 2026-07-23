package upce.fei.garden.model;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;

/**
 * Zákazník – vlastník zahrady.
 * Vytváří zahrady, zadává poptávky, schvaluje návrhy a provádí platby.
 */

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Owner extends User {
    @OneToMany(mappedBy = "owner")
    private List<Garden> gardens = new ArrayList<>();
}
