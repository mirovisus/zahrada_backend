package upce.fei.garden.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import upce.fei.garden.model.ServiceType;

import java.util.List;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {
    List<ServiceType> findAllByIdIn(List<Long> ids);
}
