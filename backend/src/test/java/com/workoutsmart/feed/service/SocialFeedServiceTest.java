package com.workoutsmart.feed.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.feed.dto.FeedPageResponse;
import com.workoutsmart.feed.dto.LikeResponse;
import com.workoutsmart.feed.dto.PostResponse;
import com.workoutsmart.feed.entity.CommunityPost;
import com.workoutsmart.feed.entity.PostLike;
import com.workoutsmart.feed.repository.CommunityPostRepository;
import com.workoutsmart.feed.repository.PostCommentRepository;
import com.workoutsmart.feed.repository.PostLikeRepository;
import com.workoutsmart.social.repository.FriendshipRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class SocialFeedServiceTest {

    @Mock
    private CommunityPostRepository postRepository;
    @Mock
    private PostLikeRepository likeRepository;
    @Mock
    private PostCommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private SeaweedStorageService storageService;

    private SocialFeedService service;

    @BeforeEach
    void setUp() {
        service = new SocialFeedService(postRepository, likeRepository, commentRepository,
                userRepository, friendshipRepository, storageService);
    }

    private User user(long id, String name, AccountStatus status) {
        return User.builder().id(id).email("u" + id + "@e.c").role("user")
                .displayName(name).accountStatus(status).build();
    }

    private CommunityPost post(long id, long userId, String audience, String content) {
        return CommunityPost.builder().id(id).userId(userId).content(content)
                .audience(audience).createdAt(Instant.now()).build();
    }

    @Test
    void createPostRejectsEmptyContentWithoutMedia() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createPost(1L, "   ", "public", null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void createPostRejectsInvalidAudience() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createPost(1L, "Nội dung", "everyone", null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void createPostSavesTextPost() {
        when(postRepository.save(any())).thenAnswer(inv -> {
            CommunityPost p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });
        when(userRepository.findAllById(any())).thenReturn(List.of(user(1L, "An", AccountStatus.ACTIVE)));

        PostResponse res = service.createPost(1L, "  Xin chào  ", "public", null);

        assertEquals("Xin chào", res.content());
        assertEquals("public", res.audience());
        assertEquals(0, res.likeCount());
        assertFalse(res.likedByMe());
        verify(storageService).store(null); // không có file → store nhận null và trả null
        ArgumentCaptor<CommunityPost> captor = ArgumentCaptor.forClass(CommunityPost.class);
        verify(postRepository).save(captor.capture());
        assertNull(captor.getValue().getMediaUrl());
    }

    @Test
    void feedExcludesPostsFromInactiveAuthors() {
        when(friendshipRepository.findAcceptedFor(1L)).thenReturn(List.of());
        when(postRepository.findFeed(eq(1L), eq(List.of(-1L)), eq(null), any(Pageable.class)))
                .thenReturn(List.of(post(1L, 2L, "public", "Bài của người bị khóa"),
                        post(2L, 3L, "public", "Bài bình thường")));
        when(userRepository.findAllById(any()))
                .thenReturn(List.of(user(2L, "Banned", AccountStatus.BANNED),
                        user(3L, "Bình", AccountStatus.ACTIVE)));
        when(likeRepository.findByPostIdIn(any())).thenReturn(List.of());
        when(commentRepository.findByPostIdIn(any())).thenReturn(List.of());

        FeedPageResponse res = service.feed(1L, "friends", null);

        assertEquals(1, res.items().size());
        assertEquals("Bài bình thường", res.items().get(0).content());
        assertEquals("Bình", res.items().get(0).displayName());
    }

    @Test
    void feedSetsCursorWhenMoreThanOnePage() {
        when(friendshipRepository.findAcceptedFor(1L)).thenReturn(List.of());
        List<CommunityPost> fetched = new ArrayList<>();
        for (long i = 21; i >= 11; i--) {
            fetched.add(post(i, 2L, "public", "Bài " + i));
        }
        when(postRepository.findFeed(eq(1L), eq(List.of(-1L)), eq(null), any(Pageable.class)))
                .thenReturn(fetched);
        when(userRepository.findAllById(any()))
                .thenReturn(List.of(user(2L, "Bình", AccountStatus.ACTIVE)));
        when(likeRepository.findByPostIdIn(any())).thenReturn(List.of());
        when(commentRepository.findByPostIdIn(any())).thenReturn(List.of());

        FeedPageResponse res = service.feed(1L, "friends", null);

        assertEquals(10, res.items().size());
        assertEquals(12L, res.nextCursor()); // id bài cuối trang (21..12) = cursor trang sau
    }

    @Test
    void toggleLikeAddsThenRemoves() {
        when(postRepository.existsById(5L)).thenReturn(true);
        when(likeRepository.findByPostIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
        when(likeRepository.countByPostId(5L)).thenReturn(1L);

        LikeResponse first = service.toggleLike(1L, 5L);
        assertTrue(first.liked());
        assertEquals(1, first.likeCount());
        verify(likeRepository).save(any());

        when(likeRepository.findByPostIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(PostLike.builder().id(9L).postId(5L).userId(1L).build()));
        when(likeRepository.countByPostId(5L)).thenReturn(0L);

        LikeResponse second = service.toggleLike(1L, 5L);
        assertFalse(second.liked());
        assertEquals(0, second.likeCount());
    }

    @Test
    void addCommentRejectsBlankContent() {
        when(postRepository.existsById(5L)).thenReturn(true);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.addComment(1L, 5L, "   "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void deletePostForbiddenForNonOwner() {
        when(postRepository.findById(5L)).thenReturn(Optional.of(post(5L, 2L, "public", "Bài")));

        ApiException ex = assertThrows(ApiException.class, () -> service.deletePost(1L, 5L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(postRepository, never()).delete(any());
    }

    @Test
    void deletePostRemovesMediaForOwner() {
        CommunityPost p = post(5L, 1L, "public", "Bài");
        p.setMediaUrl("workoutsmart-media/abc.png");
        when(postRepository.findById(5L)).thenReturn(Optional.of(p));

        service.deletePost(1L, 5L);

        verify(storageService).delete("workoutsmart-media/abc.png");
        verify(postRepository).delete(p);
    }

    @Test
    void createPostWithMediaUsesStorage() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(storageService.store(file))
                .thenReturn(new SeaweedStorageService.StoredMedia("workoutsmart-media/x.png", "image"));
        when(postRepository.save(any())).thenAnswer(inv -> {
            CommunityPost p = inv.getArgument(0);
            p.setId(11L);
            return p;
        });
        when(userRepository.findAllById(any())).thenReturn(List.of(user(1L, "An", AccountStatus.ACTIVE)));

        PostResponse res = service.createPost(1L, "", "friends", file);

        assertEquals("image", res.mediaType());
        assertEquals("workoutsmart-media/x.png", res.mediaUrl());
        verify(storageService).store(file);
    }
}
