package upce.fei.garden.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import upce.fei.garden.model.Garden;

import java.util.Optional;

public interface GardenRepository extends JpaRepository<Garden, Long> {
    Page<Garden> findAllByOwnerId(Long ownerId, Pageable pageable);

    // pro overeni vlastnictvi
    Optional<Garden> findByIdAndOwnerId(Long id, Long ownerId);
}
