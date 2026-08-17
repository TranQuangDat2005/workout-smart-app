package com.workoutsmart.social.repository;

import com.workoutsmart.social.entity.Challenge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByStatusOrderByStartDateAsc(String status);
}
