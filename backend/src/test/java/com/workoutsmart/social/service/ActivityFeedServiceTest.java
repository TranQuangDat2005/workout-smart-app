package com.workoutsmart.social.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.social.entity.ActivityFeedItem;
import com.workoutsmart.social.repository.ActivityFeedRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityFeedServiceTest {

    @Mock
    private ActivityFeedRepository feedRepository;

    private ActivityFeedService service;

    @BeforeEach
    void setUp() {
        service = new ActivityFeedService(feedRepository);
    }

    private ActivityFeedItem item(String actionType, String detailsJson) {
        return ActivityFeedItem.builder().id(1L).userId(1L)
                .actionType(actionType).detailsJson(detailsJson).build();
    }

    @Test
    void publishStreakMilestone_emitsWhenStreakIncreases() {
        when(feedRepository.findFirstByUserIdAndActionTypeOrderByIdDesc(1L, "streak_milestone"))
                .thenReturn(Optional.of(item("streak_milestone", "{\"streakWeeks\":2}")));

        service.publishStreakMilestone(1L, 3);

        verify(feedRepository, times(1)).save(any());
        var captor = ArgumentCaptor.forClass(ActivityFeedItem.class);
        verify(feedRepository).save(captor.capture());
        assertTrue(captor.getValue().getDetailsJson().contains("\"streakWeeks\":3"));
    }

    @Test
    void publishStreakMilestone_skipsWhenNotIncreased() {
        when(feedRepository.findFirstByUserIdAndActionTypeOrderByIdDesc(1L, "streak_milestone"))
                .thenReturn(Optional.of(item("streak_milestone", "{\"streakWeeks\":3}")));

        service.publishStreakMilestone(1L, 3);

        verify(feedRepository, never()).save(any());
    }

    @Test
    void publishStreakMilestone_skipsWhenZeroStreak() {
        service.publishStreakMilestone(1L, 0);
        verify(feedRepository, never()).save(any());
    }

    @Test
    void publishStreakMilestone_emitsExtraForReachedMilestones() {
        when(feedRepository.findFirstByUserIdAndActionTypeOrderByIdDesc(1L, "streak_milestone"))
                .thenReturn(Optional.of(item("streak_milestone", "{\"streakWeeks\":9}")));

        service.publishStreakMilestone(1L, 10);

        // 1 event tăng chuỗi + 1 event mốc 10
        verify(feedRepository, times(2)).save(any());
    }

    @Test
    void publishStreakMilestone_emitsAllMilestonesWhenJumping() {
        when(feedRepository.findFirstByUserIdAndActionTypeOrderByIdDesc(1L, "streak_milestone"))
                .thenReturn(Optional.empty());

        service.publishStreakMilestone(1L, 31);

        // tăng chuỗi + mốc 10 + mốc 30 = 3 events
        verify(feedRepository, times(3)).save(any());
    }

    @Test
    void publishPr_emitsWhenVolumeExceedsRecord() {
        when(feedRepository.findFirstByUserIdAndActionTypeOrderByIdDesc(1L, "new_pr"))
                .thenReturn(Optional.of(item("new_pr", "{\"volumeKg\":200}")));

        service.publishPr(1L, new BigDecimal("250"));

        verify(feedRepository, times(1)).save(any());
        var captor = ArgumentCaptor.forClass(ActivityFeedItem.class);
        verify(feedRepository).save(captor.capture());
        assertTrue(captor.getValue().getDetailsJson().contains("\"volumeKg\":250"));
    }

    @Test
    void publishPr_skipsWhenNotRecord() {
        when(feedRepository.findFirstByUserIdAndActionTypeOrderByIdDesc(1L, "new_pr"))
                .thenReturn(Optional.of(item("new_pr", "{\"volumeKg\":250}")));

        service.publishPr(1L, new BigDecimal("250"));

        verify(feedRepository, never()).save(any());
    }

    @Test
    void publishPr_skipsWhenZeroVolume() {
        service.publishPr(1L, BigDecimal.ZERO);
        verify(feedRepository, never()).save(any());
    }
}
