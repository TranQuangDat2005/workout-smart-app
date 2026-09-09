package com.workoutsmart.feed.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.feed.dto.FeedPageResponse;
import com.workoutsmart.feed.entity.CommunityPost;
import com.workoutsmart.feed.repository.CommunityPostRepository;
import com.workoutsmart.feed.repository.PostCommentRepository;
import com.workoutsmart.feed.repository.PostLikeRepository;
import com.workoutsmart.social.repository.FriendshipRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SocialFeedServicePrivacyTest {

    @Mock private CommunityPostRepository postRepository;
    @Mock private PostLikeRepository likeRepository;
    @Mock private PostCommentRepository commentRepository;
    @Mock private com.workoutsmart.auth.repository.UserRepository userRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private SeaweedStorageService storageService;

    private SocialFeedService service;

    @BeforeEach
    void setUp() {
        service = new SocialFeedService(postRepository, likeRepository, commentRepository,
                userRepository, friendshipRepository, storageService);
    }

    private CommunityPost post(long id, long userId, String audience, String content) {
        return CommunityPost.builder().id(id).userId(userId).content(content)
                .audience(audience).createdAt(Instant.now()).build();
    }

    private User user(long id, String name, boolean isPrivate) {
        return User.builder().id(id).email("u" + id + "@e.c").role("user")
                .displayName(name).accountStatus(AccountStatus.ACTIVE)
                .isPrivate(isPrivate).build();
    }

    @Test
    void feed_discover_excludesPrivateUserPosts() {
        // Private user posts should be filtered out by the repository query (isPrivate=true)
        // The mock returns only non-private user posts (simulating the updated JPQL)
        CommunityPost publicPost = post(1L, 10L, "public", "Public post by active user");

        when(postRepository.findDiscover(any(), any(Pageable.class)))
                .thenReturn(List.of(publicPost));
        when(likeRepository.findByPostIdIn(any())).thenReturn(List.of());
        when(commentRepository.findByPostIdIn(any())).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of(user(10L, "Active", false)));

        FeedPageResponse result = service.feed(1L, "discover", null);

        assertEquals(1, result.items().size());
        assertEquals("Public post by active user", result.items().get(0).content());
    }

    @Test
    void feed_discover_emptyWhenOnlyPrivateUsersPost() {
        // Repository returns empty list (private users' posts excluded by JPQL)
        when(postRepository.findDiscover(any(), any(Pageable.class)))
                .thenReturn(List.of());

        FeedPageResponse result = service.feed(1L, "discover", null);

        assertEquals(0, result.items().size());
    }

    @Test
    void feed_friends_privateUserPostsVisibleToFriends() {
        // Friends tab shows posts from friends (including private users' posts to friends)
        CommunityPost friendPost = post(1L, 20L, "public", "Friend's post");

        when(friendshipRepository.findAcceptedFor(1L)).thenReturn(List.of());
        when(postRepository.findFeed(eq(1L), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(friendPost));
        when(likeRepository.findByPostIdIn(any())).thenReturn(List.of());
        when(commentRepository.findByPostIdIn(any())).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of(user(20L, "Friend", true)));

        FeedPageResponse result = service.feed(1L, "friends", null);

        assertEquals(1, result.items().size());
        assertEquals("Friend's post", result.items().get(0).content());
    }
}
