package backend.repository;

import backend.model.AdvancedSimulationRun;
import backend.model.AlgorithmType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvancedSimulationRunRepository extends JpaRepository<AdvancedSimulationRun, Long> {

    List<AdvancedSimulationRun> findAllByOrderByCreatedAtDesc();

    List<AdvancedSimulationRun> findByAlgorithmTypeOrderByCreatedAtDesc(AlgorithmType algorithmType);
}
