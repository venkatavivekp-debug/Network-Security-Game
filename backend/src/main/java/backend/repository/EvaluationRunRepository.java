package backend.repository;

import backend.model.AlgorithmType;
import backend.model.EvaluationComparisonType;
import backend.model.EvaluationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, Long> {

    List<EvaluationRun> findAllByOrderByCreatedAtDesc();

    List<EvaluationRun> findByComparisonTypeOrderByCreatedAtDesc(EvaluationComparisonType comparisonType);

    List<EvaluationRun> findByAlgorithmTypeOrderByCreatedAtDesc(AlgorithmType algorithmType);

    List<EvaluationRun> findByAlgorithmTypeAndComparisonTypeOrderByCreatedAtDesc(AlgorithmType algorithmType, EvaluationComparisonType comparisonType);
}
