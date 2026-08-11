package upce.fei.garden.service;

import upce.fei.garden.dto.proposal.CreateProposalRequest;
import upce.fei.garden.dto.proposal.ProposalCommentSummary;
import upce.fei.garden.dto.proposal.ProposalSummary;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.Proposal;
import upce.fei.garden.model.ProposalComment;
import upce.fei.garden.model.Worker;
import upce.fei.garden.model.enums.ProposalStatus;

import java.util.Comparator;

/**
 * Převod mezi entitou {@link Proposal} a jejími DTO. Držen mimo {@link ProposalService},
 * aby servisní vrstva obsahovala pouze business logiku.
 */
final class ProposalMapper {

    private ProposalMapper() {
    }

    static Proposal toEntity(CreateProposalRequest request, Demand demand, Worker worker) {
        Proposal proposal = new Proposal();
        proposal.setDemand(demand);
        proposal.setWorker(worker);
        proposal.setPrice(request.getPrice());
        proposal.setDescription(request.getDescription());
        proposal.setStatus(ProposalStatus.NOVY);
        return proposal;
    }

    static void updateEntity(Proposal proposal, CreateProposalRequest request) {
        proposal.setPrice(request.getPrice());
        proposal.setDescription(request.getDescription());
    }

    static ProposalSummary toSummary(Proposal proposal) {
        Worker worker = proposal.getWorker();
        return new ProposalSummary(
                proposal.getId(),
                proposal.getDemand().getId(),
                worker != null ? worker.getFirstName() : null,
                worker != null ? worker.getLastName() : null,
                worker != null ? worker.getAvatarUrl() : null,
                worker != null ? worker.getBio() : null,
                proposal.getDescription(),
                proposal.getPrice(),
                proposal.getStatus(),
                proposal.getCreatedAt(),
                proposal.getComments().stream()
                        .sorted(Comparator.comparing(ProposalComment::getCreatedAt))
                        .map(comment -> new ProposalCommentSummary(comment.getText(), comment.getCreatedAt()))
                        .toList());
    }
}
