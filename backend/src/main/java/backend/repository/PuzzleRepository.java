package backend.repository;

import backend.model.Puzzle;
import backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PuzzleRepository extends JpaRepository<Puzzle, Long> {
    Optional<Puzzle> findByMessage(Message message);
    Optional<Puzzle> findByMessageId(Long messageId);
}

