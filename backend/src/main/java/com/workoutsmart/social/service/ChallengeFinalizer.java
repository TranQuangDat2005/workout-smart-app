package com.workoutsmart.social.service;

import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.social.entity.Challenge;
import com.workoutsmart.social.entity.ChallengeParticipant;
import com.workoutsmart.social.repository.ChallengeParticipantRepository;
import com.workoutsmart.social.repository.ChallengeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tự động tổng kết challenge khi hết hạn (FR-010, FR-011).
 * Chạy mỗi 60 giây: chuyển open → finished, tính final_rank theo streak tại end_date.
 */
@Service
public class ChallengeFinalizer {

    private static final Logger log = LoggerFactory.getLogger(ChallengeFinalizer.class);
    private static final ZoneId ZONE = ZoneId.systemDefault();

    /** Tie-break: streak DESC → streakStartWeek ASC (nulls last) → userId ASC. */
    private static final Comparator<ParticipantResult> COMPARATOR = Comparator
            .comparingInt((ParticipantResult r) -> r.streak().currentStreakWeeks()).reversed()
            .thenComparing(Comparator.comparing(r -> r.streak().streakStartWeek(),
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .thenComparingLong(r -> r.participant().getUserId());

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository participantRepository;
    private final WorkoutSessionRepository sessionRepository;

    public ChallengeFinalizer(ChallengeRepository challengeRepository,
                              ChallengeParticipantRepository participantRepository,
                              WorkoutSessionRepository sessionRepository) {
        this.challengeRepository = challengeRepository;
        this.participantRepository = participantRepository;
        this.sessionRepository = sessionRepository;
    }

    /** Record giữ kết quả tính streak cho 1 participant. */
    record ParticipantResult(ChallengeParticipant participant, StreakCalculator.StreakResult streak) {
    }

    /**
     * Job định kỳ: tìm challenge open đã quá end_date, tổng kết và chuyển sang finished.
     */
    @Scheduled(fixedDelay = 60 * 1000)
    @Transactional
    public void run() {
        LocalDate today = LocalDate.now(ZONE);
        List<Challenge> expired = challengeRepository.findByStatus("open").stream()
                .filter(c -> c.getEndDate() != null && c.getEndDate().isBefore(today))
                .toList();

        for (Challenge c : expired) {
            try {
                finalizeOne(c);
            } catch (Exception e) {
                log.warn("Không thể tổng kết challenge {}: {}", c.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Tổng kết 1 challenge: tính streak tại end_date, gán finalRank, chuyển finished.
     */
    @Transactional
    void finalizeOne(Challenge challenge) {
        List<ChallengeParticipant> participants =
                participantRepository.findByChallengeId(challenge.getId());

        // Tính streak cho mỗi participant (chỉ xét sessions ≤ end_date — FR-011)
        LocalDate endDate = challenge.getEndDate();
        List<ParticipantResult> ranked = participants.stream()
                .map(p -> {
                    List<Instant> starts = sessionRepository
                            .findByUserIdAndStatus(p.getUserId(), "completed")
                            .stream()
                            .filter(s -> {
                                LocalDate sessionDate = s.getStartTime().atZone(ZONE).toLocalDate();
                                return !sessionDate.isAfter(endDate);
                            })
                            .map(WorkoutSession::getStartTime)
                            .toList();
                    StreakCalculator.StreakResult streak = new StreakCalculator().calculate(starts, ZONE);
                    return new ParticipantResult(p, streak);
                })
                .sorted(COMPARATOR)
                .toList();

        // Gán finalRank + completedAt
        int rank = 1;
        for (ParticipantResult pr : ranked) {
            pr.participant().setFinalRank(rank++);
            pr.participant().setCompletedAt(Instant.now());
        }
        participantRepository.saveAll(participants);

        // Chuyển trạng thái → finished
        challenge.setStatus("finished");
        challengeRepository.save(challenge);
    }
}
