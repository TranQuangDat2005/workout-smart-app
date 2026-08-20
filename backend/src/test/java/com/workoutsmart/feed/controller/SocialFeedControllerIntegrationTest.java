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
import com.workoutsmart.social.repository.FriendshipRepository;
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

    private void cleanFeedTables() {
        commentRepository.deleteAll();
        likeRepository.deleteAll();
        postRepository.deleteAll();
        friendshipRepository.deleteAll();
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
}
