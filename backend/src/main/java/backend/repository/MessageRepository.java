package backend.repository;

import backend.model.Message;
import backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByReceiverOrderByCreatedAtDesc(User receiver);

    Optional<Message> findByIdAndReceiver(Long id, User receiver);
}
