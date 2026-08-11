package upce.fei.garden.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import upce.fei.garden.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByDemandId(Long demandId);
    List<Review> findAllByWorkerId(Long workerId);

    /**
     * Průměr hodnocení všech {@link Review} daného zahradníka - počítá se vždy znovu nad všemi
     * záznamy (ne inkrementálně), aby se {@code averageRating} nemohl v čase rozejít se skutečnými
     * daty. Vrátí {@code null}, pokud zahradník ještě nemá žádné hodnocení.
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.worker.id = :workerId")
    Double findAverageRatingByWorkerId(@Param("workerId") Long workerId);
}
