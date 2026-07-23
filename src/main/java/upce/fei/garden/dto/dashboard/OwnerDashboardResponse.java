package upce.fei.garden.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import upce.fei.garden.dto.profile.OwnerProfileResponse;

import java.util.List;

/**
 * Data pro dashboard vlastníka zahrady – profil a seznam zahrad.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDashboardResponse {
    private OwnerProfileResponse profile;
    private List<GardenSummary> gardens;
}
