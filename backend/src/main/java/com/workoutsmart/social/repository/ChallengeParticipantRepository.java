package com.workoutsmart.social.repository;

import com.workoutsmart.social.entity.ChallengeParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, Long> {

    Optional<ChallengeParticipant> findByChallengeIdAndUserId(Long challengeId, Long userId);

    List<ChallengeParticipant> findByUserId(Long userId);
}
