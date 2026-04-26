package backend.repository;

import backend.model.UserBehaviorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBehaviorProfileRepository extends JpaRepository<UserBehaviorProfile, Long> {

    Optional<UserBehaviorProfile> findByUserId(Long userId);

    /**
     * Returns profiles sorted by consecutive failures (descending) so the admin SOC can
     * surface "users currently struggling" at the top of the list.
     */
    List<UserBehaviorProfile> findTop50ByOrderByConsecutiveFailuresDescLastFailureAtDesc();
}
