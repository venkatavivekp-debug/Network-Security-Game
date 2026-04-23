package backend.repository;

import backend.model.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {
}

