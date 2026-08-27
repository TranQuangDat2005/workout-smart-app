package com.workoutsmart.feed.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.OtpVerificationRepository;
import com.workoutsmart.auth.repository.RefreshTokenRepository;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.feed.repository.CommunityPostRepository;
import com.workoutsmart.feed.repository.PostCommentRepository;
import com.workoutsmart.feed.repository.PostLikeRepository;
import com.workoutsmart.feed.service.SeaweedStorageService;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.entity.WorkoutSet;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import com.workoutsmart.social.entity.Friendship;
import com.workoutsmart.social.repository.ActivityFeedRepository;
import com.workoutsmart.social.repository.FriendshipRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialFeedControllerIntegrationTest {

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
    private ActivityFeedRepository activityFeedRepository;
    @Autowired
    private WorkoutSessionRepository sessionRepository;
    @Autowired
    private WorkoutSetRepository setRepository;
    @Autowired
    private CommunityPostRepository postRepository;
    @Autowired
    private PostLikeRepository likeRepository;
    @Autowired
    private PostCommentRepository commentRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private SeaweedStorageService storageService;

    @BeforeEach
    void clean() {
        cleanFeedTables();
        when(storageService.store(any()))
                .thenReturn(new SeaweedStorageService.StoredMedia("workoutsmart-media/test.png", "image"));
    }

    /** Dọn sau mỗi test — DB H2 chia sẻ giữa các class nên không được để sót dữ liệu feed. */
    @AfterEach
    void cleanupAfter() {
        cleanFeedTables();
    }

    @Autowired
    private com.workoutsmart.social.repository.LeaderboardRepository leaderboardRepository;

    private void cleanFeedTables() {
        commentRepository.deleteAll();
        likeRepository.deleteAll();
        postRepository.deleteAll();
        activityFeedRepository.deleteAll();
        friendshipRepository.deleteAll();
        leaderboardRepository.deleteAll();
        setRepository.deleteAll();
        sessionRepository.deleteAll();
        refreshTokenRepository.deleteAll();
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

    private long createPost(String token, String content, String audience) throws Exception {
        MvcResult res = mockMvc.perform(multipart("/api/v1/feed/posts")
                        .param("content", content)
                        .param("audience", audience)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void postFeedLikeCommentDeleteFlow() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        long postId = createPost(tokenA, "Bài đầu tiên", "public");

        mockMvc.perform(get("/api/v1/feed/posts").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].content").value("Bài đầu tiên"))
                .andExpect(jsonPath("$.items[0].displayName").value("An"))
                .andExpect(jsonPath("$.items[0].likeCount").value(0));

        // Like → unlike
        mockMvc.perform(post("/api/v1/feed/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));
        mockMvc.perform(post("/api/v1/feed/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));

        // Comment
        mockMvc.perform(post("/api/v1/feed/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hay quá\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hay quá"));

        mockMvc.perform(get("/api/v1/feed/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Feed kèm comment
        mockMvc.perform(get("/api/v1/feed/posts").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].comments.length()").value(1));

        // Xóa bài của chính mình
        mockMvc.perform(delete("/api/v1/feed/posts/" + postId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/feed/posts").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void audienceVisibility() throws Exception {
        createUser("a@example.com", "An");
        createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");
        String tokenB = login("b@example.com");

        createPost(tokenA, "Bí mật", "private");
        createPost(tokenA, "Công khai", "public");

        // B không phải bạn bè → chỉ thấy bài public
        mockMvc.perform(get("/api/v1/feed/posts").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].content").value("Công khai"));

        // Discover cũng chỉ toàn public
        mockMvc.perform(get("/api/v1/feed/posts?tab=discover").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].content").value("Công khai"));

        // A thấy cả 2 bài của mình
        mockMvc.perform(get("/api/v1/feed/posts").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void deleteForbiddenForNonOwner() throws Exception {
        createUser("a@example.com", "An");
        createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");
        String tokenB = login("b@example.com");

        long postId = createPost(tokenA, "Bài của An", "public");

        mockMvc.perform(delete("/api/v1/feed/posts/" + postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/feed/posts/" + postId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());
    }

    @Test
    void createPostWithMediaStoresViaSeaweed() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        MockMultipartFile media = new MockMultipartFile(
                "media", "pic.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/feed/posts")
                        .file(media)
                        .param("content", "Ảnh đẹp")
                        .param("audience", "public")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Ảnh đẹp"))
                .andExpect(jsonPath("$.mediaType").value("image"))
                .andExpect(jsonPath("$.mediaUrl").value("workoutsmart-media/test.png"));
    }

    @Test
    void createPostRejectsEmptyPayload() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        mockMvc.perform(multipart("/api/v1/feed/posts")
                        .param("content", "   ")
                        .param("audience", "public")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void commentValidation() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");
        long postId = createPost(tokenA, "Bài", "public");

        mockMvc.perform(post("/api/v1/feed/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completingSessionPublishesAchievementEventsToFriendFeed() throws Exception {
        Long idA = createUser("a@example.com", "An");
        Long idB = createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");
        String tokenB = login("b@example.com");

        // A và B là bạn bè → B thấy feed của A (FR-005/FR-006)
        friendshipRepository.save(Friendship.builder()
                .userId1(idA).userId2(idB).status("accepted").initiatedBy(idA).build());

        // A đã hoàn thành 2 buổi tuần này → streak sắp tăng lên 1
        for (int i = 0; i < 2; i++) {
            sessionRepository.save(WorkoutSession.builder().userId(idA).status("completed")
                    .startTime(Instant.now().minus(i + 1, ChronoUnit.HOURS)).build());
        }
        // Buổi đang tập với 1 set 20kg × 10 reps (volume 200kg — kỷ lục đầu tiên)
        Long activeId = sessionRepository.save(WorkoutSession.builder().userId(idA)
                .status("active").startTime(Instant.now()).build()).getId();
        setRepository.save(WorkoutSet.builder().sessionId(activeId)
                .weightUsed(new BigDecimal("20")).repsCompleted(10).build());

        // A hoàn thành buổi → phát streak_milestone + new_pr
        mockMvc.perform(post("/api/v1/workout-sessions/" + activeId + "/complete")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // B xem feed → thấy cả 2 sự kiện thành tích của A
        MvcResult feed = mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        var items = objectMapper.readTree(feed.getResponse().getContentAsString());
        List<String> types = new ArrayList<>();
        items.forEach(n -> types.add(n.get("actionType").asText()));
        org.junit.jupiter.api.Assertions.assertTrue(types.contains("streak_milestone"),
                "Feed phải chứa streak_milestone: " + types);
        org.junit.jupiter.api.Assertions.assertTrue(types.contains("new_pr"),
                "Feed phải chứa new_pr: " + types);
    }

    @Test
    void completingSessionDoesNotEmitPlainWorkoutEvent() throws Exception {
        Long idA = createUser("a@example.com", "An");
        createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");

        Long activeId = sessionRepository.save(WorkoutSession.builder().userId(idA)
                .status("active").startTime(Instant.now()).build()).getId();

        mockMvc.perform(post("/api/v1/workout-sessions/" + activeId + "/complete")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Q5=B: KHÔNG phát workout_completed; buổi đầu (0 set) không tạo PR
        List<Long> all = new ArrayList<>();
        activityFeedRepository.findAll().forEach(i -> all.add(i.getId()));
        org.junit.jupiter.api.Assertions.assertTrue(all.isEmpty(),
                "Không được có event feed cho buổi tập thường: " + all);
    }

    @Test
    void createPostWithFileDelegatesToStorageService() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        MockMultipartFile png = new MockMultipartFile("media", "photo.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        mockMvc.perform(multipart("/api/v1/feed/posts")
                        .file(png)
                        .param("content", "Ảnh test")
                        .param("audience", "public")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaUrl").isNotEmpty());

        org.mockito.Mockito.verify(storageService).store(any());
    }

    @Test
    void createPostAcceptsGifUrlFromTenor() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        mockMvc.perform(multipart("/api/v1/feed/posts")
                        .param("content", "Funny GIF")
                        .param("audience", "public")
                        .param("gifUrl", "https://tenor.com/view/cat-funny-cat-gif-12345678")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gifUrl").value("https://tenor.com/view/cat-funny-cat-gif-12345678"));
    }

    @Test
    void createPostAcceptsGifUrlFromMediaTenorCdn() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        mockMvc.perform(multipart("/api/v1/feed/posts")
                        .param("content", "GIF CDN")
                        .param("audience", "public")
                        .param("gifUrl", "https://media.tenor.com/view/cat-gif-12345678")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gifUrl").value("https://media.tenor.com/view/cat-gif-12345678"));
    }

    @Test
    void createPostRejectsGifUrlOver500() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        String tooLong = "https://media.tenor.com/" + "a".repeat(600);

        mockMvc.perform(multipart("/api/v1/feed/posts")
                        .param("content", "GIF quá dài")
                        .param("audience", "public")
                        .param("gifUrl", tooLong)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPostRejectsMediaAndGifTogether() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        MockMultipartFile media = new MockMultipartFile(
                "media", "pic.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/feed/posts")
                        .file(media)
                        .param("content", "Vừa ảnh vừa GIF")
                        .param("audience", "public")
                        .param("gifUrl", "https://tenor.com/view/some-gif")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void likeAndCommentsBlockedForPrivatePostOfStranger() throws Exception {
        createUser("a@example.com", "An");
        createUser("b@example.com", "Bình");
        String tokenA = login("a@example.com");
        String tokenB = login("b@example.com");

        long postId = createPost(tokenA, "Chỉ mình An", "private");

        // B không phải tác giả → FR-AUDIENCE: like/comments/đọc comments đều 403
        mockMvc.perform(post("/api/v1/feed/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/feed/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/feed/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Không nên thấy\"}"))
                .andExpect(status().isForbidden());

        // Chính tác giả still OK
        mockMvc.perform(get("/api/v1/feed/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    void createPostRejectsGifUrlFromUnknownHost() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        mockMvc.perform(multipart("/api/v1/feed/posts")
                        .param("content", "Evil GIF")
                        .param("audience", "public")
                        .param("gifUrl", "https://evil.com/malicious.gif")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPostRejectsGifUrlWithHttp() throws Exception {
        createUser("a@example.com", "An");
        String tokenA = login("a@example.com");

        mockMvc.perform(multipart("/api/v1/feed/posts")
                        .param("content", "HTTP GIF")
                        .param("audience", "public")
                        .param("gifUrl", "http://tenor.com/view/some-gif")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }
}
