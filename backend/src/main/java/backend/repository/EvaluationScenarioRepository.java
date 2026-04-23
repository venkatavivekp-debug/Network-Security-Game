package backend.repository;

import backend.model.EvaluationScenario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationScenarioRepository extends JpaRepository<EvaluationScenario, Long> {
}
