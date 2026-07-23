package upce.fei.garden.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import upce.fei.garden.dto.profile.WorkerProfileResponse;
import upce.fei.garden.dto.proposal.ProposalSummary;

import java.util.List;

/**
 * Data pro dashboard zahradníka – profil, hodnocení a seznam návrhů.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WorkerDashboardResponse {
    private WorkerProfileResponse profile;
    private Double averageRating;
    private List<ProposalSummary> proposals;
}
