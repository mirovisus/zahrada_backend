package upce.fei.garden.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import upce.fei.garden.model.ProposalComment;

public interface ProposalCommentRepository extends JpaRepository<ProposalComment, Long> {
}
