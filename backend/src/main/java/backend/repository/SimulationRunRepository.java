package backend.repository;

import backend.model.AlgorithmType;
import backend.model.SimulationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationRunRepository extends JpaRepository<SimulationRun, Long> {

    List<SimulationRun> findAllByOrderByCreatedAtDesc();

    List<SimulationRun> findByAlgorithmTypeOrderByCreatedAtDesc(AlgorithmType algorithmType);
}
