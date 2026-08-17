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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
    private ChallengeRepository challengeRepository;
    @Autowired
    private ChallengeParticipantRepository participantRepository;
    @Autowired
    private WorkoutSessionRepository sessionRepository;
    @Autowired
    private WorkoutSetRepository setRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

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
        Long idB = createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");

        // A: 3 buổi completed hôm nay → streak ≥ 1 tuần này (không phụ thuộc thứ trong tuần)
        for (int i = 0; i < 3; i++) {
            sessionRepository.save(WorkoutSession.builder()
                    .userId(idA).status("completed")
                    .startTime(Instant.now().minus(i, ChronoUnit.HOURS))
                    .build());
        }

        mockMvc.perform(get("/api/v1/leaderboard")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("An"))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].currentStreakWeeks").value(1));

        org.junit.jupiter.api.Assertions.assertEquals(2, leaderboardRepository.count());
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
}
