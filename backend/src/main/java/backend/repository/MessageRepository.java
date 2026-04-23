package backend.repository;

import backend.model.Message;
import backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByReceiverOrderByCreatedAtDesc(User receiver);

    Optional<Message> findByIdAndReceiver(Long id, User receiver);

    @Query("select m from Message m where m.id = :id and (m.sender = :user or m.receiver = :user)")
    Optional<Message> findByIdAndParticipant(@Param("id") Long id, @Param("user") User user);
}
