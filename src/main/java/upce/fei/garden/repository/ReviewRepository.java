package upce.fei.garden.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import upce.fei.garden.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByDemandId(Long demandId);
    List<Review> findAllByWorkerId(Long workerId);
}
