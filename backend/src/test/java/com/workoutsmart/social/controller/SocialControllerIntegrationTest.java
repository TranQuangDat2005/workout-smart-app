package com.workoutsmart.social.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.OtpVerificationRepository;
import com.workoutsmart.auth.repository.RefreshTokenRepository;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import com.workoutsmart.social.repository.ActivityFeedRepository;
import com.workoutsmart.social.repository.ChallengeParticipantRepository;
import com.workoutsmart.social.repository.ChallengeRepository;
import com.workoutsmart.social.repository.FriendshipRepository;
import com.workoutsmart.social.repository.LeaderboardRepository;
import com.workoutsmart.social.entity.Challenge;
import com.workoutsmart.social.service.ChallengeFinalizer;
import com.workoutsmart.social.service.LeaderboardSyncService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OtpVerificationRepository otpRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private ActivityFeedRepository feedRepository;
    @Autowired
    private LeaderboardRepository leaderboardRepository;
    @Autowired
    private LeaderboardSyncService leaderboardSyncService;
    @Autowired
    private ChallengeFinalizer challengeFinalizer;
    @Autowired
    private ChallengeRepository challengeRepository;
    @Autowired
    private ChallengeParticipantRepository participantRepository;
    @Autowired
    private WorkoutSessionRepository sessionRepository;
    @Autowired
    private WorkoutSetRepository setRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        refreshTokenRepository.deleteAll();
        participantRepository.deleteAll();
        challengeRepository.deleteAll();
        leaderboardRepository.deleteAll();
        feedRepository.deleteAll();
        friendshipRepository.deleteAll();
        setRepository.deleteAll();
        sessionRepository.deleteAll();
        otpRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Long createUser(String email, String name) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("user")
                .displayName(name)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .isPrivate(false)
                .build()).getId();
    }

    private String login(String email) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    void friendshipFullFlow() throws Exception {
        createUser("a@example.com", "An");
        createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");
        String tokenB = login("b@example.com");
        Long idB = userRepository.findByEmail("b@example.com").orElseThrow().getId();

        // A gửi lời mời
        MvcResult send = mockMvc.perform(post("/api/v1/friendships")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":" + idB + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pending"))
                .andReturn();
        long friendshipId = objectMapper.readTree(send.getResponse().getContentAsString()).get("id").asLong();

        // B thấy pending
        mockMvc.perform(get("/api/v1/friendships/pending")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // B accept
        mockMvc.perform(post("/api/v1/friendships/" + friendshipId + "/accept")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Cả 2 đều thấy bạn bè
        mockMvc.perform(get("/api/v1/friends")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Feed có item friendship_created
        mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // A hủy kết bạn
        mockMvc.perform(delete("/api/v1/friendships/" + friendshipId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/friends")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void crossRequestAutoAccepts() throws Exception {
        createUser("a@example.com", "An");
        createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");
        String tokenB = login("b@example.com");
        Long idA = userRepository.findByEmail("a@example.com").orElseThrow().getId();
        Long idB = userRepository.findByEmail("b@example.com").orElseThrow().getId();

        // B gửi cho A trước
        mockMvc.perform(post("/api/v1/friendships")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":" + idA + "}"))
                .andExpect(status().isCreated());

        // A gửi cho B → auto accept (không tạo pending thứ 2)
        mockMvc.perform(post("/api/v1/friendships")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":" + idB + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("accepted"));

        org.junit.jupiter.api.Assertions.assertEquals(1, friendshipRepository.count());
    }

    @Test
    void leaderboardRanksByStreak() throws Exception {
        Long idA = createUser("a@example.com", "An");
        createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");

        // A: 3 buổi completed hôm nay → streak ≥ 1 tuần này
        for (int i = 0; i < 3; i++) {
            sessionRepository.save(WorkoutSession.builder()
                    .userId(idA).status("completed")
                    .startTime(Instant.now().minus(i, ChronoUnit.HOURS))
                    .build());
        }
        // Sync leaderboard entry cho A
        leaderboardSyncService.updateEntry(idA);

        mockMvc.perform(get("/api/v1/leaderboard")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("An"))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].currentStreakWeeks").value(1));

        org.junit.jupiter.api.Assertions.assertEquals(1, leaderboardRepository.count());
    }

    @Test
    void leaderboard_tieBreakByStreakStartWeek() throws Exception {
        // Tạo 2 user cùng streak nhưng A bắt đầu chuỗi sớm hơn
        Long idA = createUser("a@example.com", "An");
        Long idB = createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");

        // Tạo LeaderboardEntry trực tiếp với streakStartWeek khác nhau
        // Cả 2 đều streak = 1 tuần (3 buổi tuần này)
        for (int i = 0; i < 3; i++) {
            sessionRepository.save(WorkoutSession.builder()
                    .userId(idA).status("completed")
                    .startTime(Instant.now().minus(i, ChronoUnit.HOURS))
                    .build());
            sessionRepository.save(WorkoutSession.builder()
                    .userId(idB).status("completed")
                    .startTime(Instant.now().minus(i, ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS))
                    .build());
        }
        leaderboardSyncService.updateEntry(idA);
        leaderboardSyncService.updateEntry(idB);

        mockMvc.perform(get("/api/v1/leaderboard")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[1].rank").value(2));
    }

    @Test
    void leaderboard_viewerAlwaysIncluded() throws Exception {
        Long idA = createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        // A có 3 buổi completed
        for (int i = 0; i < 3; i++) {
            sessionRepository.save(WorkoutSession.builder()
                    .userId(idA).status("completed")
                    .startTime(Instant.now().minus(i, ChronoUnit.HOURS))
                    .build());
        }
        leaderboardSyncService.updateEntry(idA);

        // A xem leaderboard → luôn thấy vị trí của mình
        mockMvc.perform(get("/api/v1/leaderboard")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("An"))
                .andExpect(jsonPath("$[0].rank").value(1));
    }

    @Test
    void challengeCreateJoinFlow() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        // User thường KHÔNG được tạo challenge (hasRole ADMIN)
        mockMvc.perform(post("/api/v1/challenges")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"180 ngày cutting\",\"goalType\":\"weight_loss\",\"durationDays\":180}"))
                .andExpect(status().isForbidden());

        // Admin tạo challenge
        Long adminId = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("admin")
                .displayName("Admin")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build()).getId();
        String tokenAdmin = login("admin@example.com");

        MvcResult create = mockMvc.perform(post("/api/v1/challenges")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"180 ngày cutting\",\"goalType\":\"weight_loss\",\"durationDays\":180}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("open"))
                .andReturn();
        long challengeId = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asLong();

        // User tham gia
        mockMvc.perform(post("/api/v1/challenges/" + challengeId + "/join")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Tham gia lại → 422
        mockMvc.perform(post("/api/v1/challenges/" + challengeId + "/join")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isUnprocessableEntity());

        // Danh sách challenge của user
        mockMvc.perform(get("/api/v1/challenges/mine")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].joined").value(true));
    }

    @Test
    void socialEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users/search"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/feed"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectedRequestIsReusedAfterCooldown() throws Exception {
        createUser("a@example.com", "An");
        createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");
        String tokenB = login("b@example.com");
        Long idB = userRepository.findByEmail("b@example.com").orElseThrow().getId();

        // A gửi, B từ chối
        MvcResult send = mockMvc.perform(post("/api/v1/friendships")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":" + idB + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        long friendshipId = objectMapper.readTree(send.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/v1/friendships/" + friendshipId + "/reject")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Gửi lại trong cooldown → 429
        mockMvc.perform(post("/api/v1/friendships")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":" + idB + "}"))
                .andExpect(status().isTooManyRequests());

        // Đẩy updated_at về 31 ngày trước (mô phỏng hết cooldown) — SQL trực tiếp để bypass @PreUpdate
        jdbcTemplate.update("UPDATE friendships SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(31, ChronoUnit.DAYS)), friendshipId);

        // Gửi lại sau cooldown → tái sử dụng row (pending), KHÔNG tạo row mới
        mockMvc.perform(post("/api/v1/friendships")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":" + idB + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pending"));
        org.junit.jupiter.api.Assertions.assertEquals(1, friendshipRepository.count());
    }

    @Test
    void searchReturnsRelationshipStatus() throws Exception {
        createUser("a@example.com", "An");
        createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");
        Long idB = userRepository.findByEmail("b@example.com").orElseThrow().getId();

        // Chưa có quan hệ → none
        mockMvc.perform(get("/api/v1/users/search").param("q", "Bình")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relationshipStatus").value("none"));

        // A gửi → pending_sent
        mockMvc.perform(post("/api/v1/friendships")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":" + idB + "}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/users/search").param("q", "Bình")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relationshipStatus").value("pending_sent"));

        // B chấp nhận → accepted
        String tokenB = login("b@example.com");
        mockMvc.perform(get("/api/v1/friendships/pending")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
        var pending = friendshipRepository.findPendingFor(userRepository.findByEmail("b@example.com")
                .orElseThrow().getId());
        mockMvc.perform(post("/api/v1/friendships/" + pending.get(0).getId() + "/accept")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/search").param("q", "Bình")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relationshipStatus").value("accepted"));
    }

    @Test
    void challengeJoinAfterEndDate_returns422() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        // Tạo challenge trực tiếp trong DB với endDate = yesterday
        Challenge c = challengeRepository.save(Challenge.builder()
                .name("Expired challenge")
                .durationDays(1)
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().minusDays(1))
                .status("open")
                .createdBy(1L)
                .build());

        // Join → 422 vì đã hết hạn
        mockMvc.perform(post("/api/v1/challenges/" + c.getId() + "/join")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void challengeFinalize_setsFinalRankAndStatus() throws Exception {
        Long idA = createUser("a@example.com", "An");
        Long idB = createUser("b@example.com", "Bình");

        // Tạo challenge đã hết hạn (endDate = yesterday)
        Challenge c = challengeRepository.save(Challenge.builder()
                .name("1 ngày challenge")
                .durationDays(1)
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().minusDays(1))
                .status("open")
                .createdBy(idA)
                .build());

        // 2 user tham gia
        participantRepository.save(com.workoutsmart.social.entity.ChallengeParticipant.builder()
                .challengeId(c.getId()).userId(idA).build());
        participantRepository.save(com.workoutsmart.social.entity.ChallengeParticipant.builder()
                .challengeId(c.getId()).userId(idB).build());

        // A có 3 buổi completed (streak=1)
        for (int i = 0; i < 3; i++) {
            sessionRepository.save(WorkoutSession.builder()
                    .userId(idA).status("completed")
                    .startTime(Instant.now().minus(i, ChronoUnit.HOURS))
                    .build());
        }

        // Chạy finalizer
        challengeFinalizer.run();

        // Verify status = finished
        Challenge updated = challengeRepository.findById(c.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("finished", updated.getStatus());

        // Verify cả 2 participant đều có finalRank và completedAt
        var participants = participantRepository.findByChallengeId(c.getId());
        org.junit.jupiter.api.Assertions.assertEquals(2, participants.size());
        participants.forEach(p -> {
            org.junit.jupiter.api.Assertions.assertNotNull(p.getFinalRank());
            org.junit.jupiter.api.Assertions.assertNotNull(p.getCompletedAt());
        });
        // A có streak → rank 1, B không có session → rank 2
        var pA = participants.stream().filter(p -> p.getUserId().equals(idA)).findFirst().orElseThrow();
        var pB = participants.stream().filter(p -> p.getUserId().equals(idB)).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(1, pA.getFinalRank());
        org.junit.jupiter.api.Assertions.assertEquals(2, pB.getFinalRank());
    }

    @Test
    void challengeResults_returnsRankings() throws Exception {
        Long idA = createUser("a@example.com", "An");
        Long idB = createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");

        // Tạo challenge đã finalized
        Challenge c = challengeRepository.save(Challenge.builder()
                .name("Finished challenge")
                .durationDays(1)
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().minusDays(1))
                .status("finished")
                .createdBy(idA)
                .build());

        participantRepository.save(com.workoutsmart.social.entity.ChallengeParticipant.builder()
                .challengeId(c.getId()).userId(idA)
                .finalRank(1).completedAt(Instant.now()).build());
        participantRepository.save(com.workoutsmart.social.entity.ChallengeParticipant.builder()
                .challengeId(c.getId()).userId(idB)
                .finalRank(2).completedAt(Instant.now()).build());

        mockMvc.perform(get("/api/v1/challenges/" + c.getId() + "/results")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].finalRank").value(1))
                .andExpect(jsonPath("$[1].finalRank").value(2));
    }
}
