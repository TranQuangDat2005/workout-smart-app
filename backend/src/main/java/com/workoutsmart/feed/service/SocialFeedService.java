package com.workoutsmart.feed.service;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.feed.dto.CommentResponse;
import com.workoutsmart.feed.dto.FeedPageResponse;
import com.workoutsmart.feed.dto.LikeResponse;
import com.workoutsmart.feed.dto.PostResponse;
import com.workoutsmart.feed.entity.CommunityPost;
import com.workoutsmart.feed.entity.PostComment;
import com.workoutsmart.feed.entity.PostLike;
import com.workoutsmart.feed.repository.CommunityPostRepository;
import com.workoutsmart.feed.repository.PostCommentRepository;
import com.workoutsmart.feed.repository.PostLikeRepository;
import com.workoutsmart.social.entity.Friendship;
import com.workoutsmart.social.repository.FriendshipRepository;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Nghiệp vụ feed cộng đồng (018): đăng bài text/media, like, comment, xóa.
 * Tách riêng khỏi SocialService (activity feed) để module gọn.
 */
@Service
public class SocialFeedService {

    private static final int PAGE_SIZE = 10;
    private static final Set<String> AUDIENCES = Set.of("public", "friends", "private");
    /** FR-015 / R5: host allowlist cho gifUrl embed — tenor.com (+ media.tenor.com) + instagram.com. */
    private static final Set<String> GIF_HOST_ALLOWLIST = Set.of(
            "tenor.com", "www.tenor.com", "media.tenor.com",
            "instagram.com", "www.instagram.com");

    private final CommunityPostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final PostCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final SeaweedStorageService storageService;

    public SocialFeedService(CommunityPostRepository postRepository,
                             PostLikeRepository likeRepository,
                             PostCommentRepository commentRepository,
                             UserRepository userRepository,
                             FriendshipRepository friendshipRepository,
                             SeaweedStorageService storageService) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.storageService = storageService;
    }

    @Transactional
    public PostResponse createPost(Long userId, String content, String audience,
                                   MultipartFile media, String gifUrl) {
        String text = content == null ? null : content.trim();
        if ((text == null || text.isEmpty()) && (media == null || media.isEmpty())
                && (gifUrl == null || gifUrl.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bài đăng cần có nội dung, media hoặc GIF");
        }
        if (text != null && text.length() > 2000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nội dung bài đăng tối đa 2000 ký tự");
        }
        String aud = audience == null ? "public" : audience.trim().toLowerCase(Locale.ROOT);
        if (!AUDIENCES.contains(aud)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "audience phải là public, friends hoặc private");
        }
        // FR-015: validate gifUrl — HTTPS + host allowlist (trước khi upload để không tốn media)
        String validatedGifUrl = validateGifUrl(gifUrl);
        boolean hasMedia = media != null && !media.isEmpty();
        // data-model.md R3: bài không mang đồng thời media + gif_url
        if (hasMedia && validatedGifUrl != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không thể đăng bài vừa có ảnh vừa có GIF");
        }

        SeaweedStorageService.StoredMedia stored = storageService.store(media);
        CommunityPost post = CommunityPost.builder()
                .userId(userId)
                .content(text == null || text.isEmpty() ? null : text)
                .mediaUrl(stored == null ? null : stored.key())
                .mediaType(stored == null ? null : stored.mediaType())
                .gifUrl(validatedGifUrl)
                .audience(aud)
                .createdAt(Instant.now())
                .build();
        post = postRepository.save(post);
        return buildResponses(List.of(post), userId).get(0);
    }

    /**
     * Feed 2 tab: "friends" (public toàn app + public/friends của bạn bè + bài của mình)
     * và "discover" (toàn bộ bài public). Cursor = id bài cuối trang trước.
     */
    @Transactional(readOnly = true)
    public FeedPageResponse feed(Long userId, String tab, Long cursor) {
        boolean discover = "discover".equalsIgnoreCase(tab == null ? "" : tab.trim());
        List<CommunityPost> fetched;
        if (discover) {
            fetched = postRepository.findDiscover(cursor, PageRequest.of(0, PAGE_SIZE + 1));
        } else {
            List<Long> friendIds = friendIdsOf(userId);
            // IN () với list rỗng không đáng tin — dùng id không tồn tại để luôn false.
            List<Long> ids = friendIds.isEmpty() ? List.of(-1L) : friendIds;
            fetched = postRepository.findFeed(userId, ids, cursor, PageRequest.of(0, PAGE_SIZE + 1));
        }

        boolean hasMore = fetched.size() > PAGE_SIZE;
        List<CommunityPost> page = hasMore ? fetched.subList(0, PAGE_SIZE) : fetched;
        Long nextCursor = hasMore ? page.get(page.size() - 1).getId() : null;

        List<CommunityPost> visible = filterActiveAuthors(page);
        return new FeedPageResponse(buildResponses(visible, userId), nextCursor);
    }

    @Transactional
    public LikeResponse toggleLike(Long userId, Long postId) {
        requireVisible(userId, postId);
        Optional<PostLike> existing = likeRepository.findByPostIdAndUserId(postId, userId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            liked = false;
        } else {
            likeRepository.save(PostLike.builder().postId(postId).userId(userId).build());
            liked = true;
        }
        return new LikeResponse(liked, likeRepository.countByPostId(postId));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> comments(Long userId, Long postId) {
        requireVisible(userId, postId);
        List<PostComment> list = commentRepository.findByPostIdOrderByIdAsc(postId);
        return toCommentResponses(list);
    }

    @Transactional
    public CommentResponse addComment(Long userId, Long postId, String content) {
        requireVisible(userId, postId);
        if (content == null || content.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nội dung bình luận không được để trống");
        }
        String text = content.trim();
        if (text.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bình luận tối đa 1000 ký tự");
        }
        PostComment saved = commentRepository.save(PostComment.builder()
                .postId(postId)
                .userId(userId)
                .content(text)
                .createdAt(Instant.now())
                .build());
        User author = userRepository.findById(userId).orElse(null);
        return toCommentResponse(saved, author);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài đăng"));
        if (!post.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa bài đăng này");
        }
        storageService.delete(post.getMediaUrl());
        postRepository.delete(post); // FK CASCADE xóa likes + comments
    }

    private List<Long> friendIdsOf(Long userId) {
        return friendshipRepository.findAcceptedFor(userId).stream()
                .map(f -> f.getUserId1().equals(userId) ? f.getUserId2() : f.getUserId1())
                .toList();
    }

    /** FR-AUDIENCE: bài đăng phải tồn tại và viewer phải có quyền xem — nếu không → 403/404. */
    private void requireVisible(Long viewerId, Long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài đăng"));
        if (!canView(viewerId, post)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem bài đăng này");
        }
    }

    /** FR-AUDIENCE: public → mọi user; bạn tác giả → xem được; friends → bạn bè hoặc tác giả; private → chỉ tác giả. */
    private boolean canView(Long viewerId, CommunityPost post) {
        if (post.getUserId().equals(viewerId)) {
            return true;
        }
        return switch (post.getAudience()) {
            case "public" -> true;
            case "friends" -> isFriend(viewerId, post.getUserId());
            default -> false; // private
        };
    }

    private boolean isFriend(Long a, Long b) {
        return friendshipRepository.findBetween(a, b).stream()
                .anyMatch(f -> "accepted".equals(f.getStatus()));
    }

    /** Ẩn bài của tác giả không còn ACTIVE (banned/deleted). */
    private List<CommunityPost> filterActiveAuthors(List<CommunityPost> posts) {
        Set<Long> authorIds = posts.stream().map(CommunityPost::getUserId).collect(Collectors.toSet());
        Set<Long> active = userRepository.findAllById(authorIds).stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
                .map(User::getId)
                .collect(Collectors.toSet());
        return posts.stream().filter(p -> active.contains(p.getUserId())).toList();
    }

    /** Batch load likes/comments + authors cho cả trang (tránh N+1). */
    private List<PostResponse> buildResponses(List<CommunityPost> posts, Long viewerId) {
        if (posts.isEmpty()) {
            return List.of();
        }
        List<Long> postIds = posts.stream().map(CommunityPost::getId).toList();
        Map<Long, List<PostLike>> likesByPost = likeRepository.findByPostIdIn(postIds).stream()
                .collect(Collectors.groupingBy(PostLike::getPostId));
        Map<Long, List<PostComment>> commentsByPost = commentRepository.findByPostIdIn(postIds).stream()
                .collect(Collectors.groupingBy(PostComment::getPostId));

        Set<Long> userIds = new HashSet<>();
        posts.forEach(p -> userIds.add(p.getUserId()));
        commentsByPost.values().stream().flatMap(List::stream).forEach(c -> userIds.add(c.getUserId()));
        Map<Long, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return posts.stream().map(post -> {
            List<PostLike> likes = likesByPost.getOrDefault(post.getId(), List.of());
            boolean likedByMe = likes.stream().anyMatch(l -> l.getUserId().equals(viewerId));
            List<CommentResponse> commentResponses = commentsByPost.getOrDefault(post.getId(), List.of())
                    .stream()
                    .map(c -> toCommentResponse(c, users.get(c.getUserId())))
                    .toList();
            User author = users.get(post.getUserId());
            return new PostResponse(
                    post.getId(),
                    post.getUserId(),
                    author == null ? null : author.getDisplayName(),
                    author == null ? null : author.getAvatarUrl(),
                    post.getContent(),
                    post.getMediaType(),
                    post.getMediaUrl(),
                    post.getGifUrl(),
                    post.getAudience(),
                    post.getCreatedAt(),
                    likes.size(),
                    likedByMe,
                    commentResponses);
        }).toList();
    }

    private List<CommentResponse> toCommentResponses(List<PostComment> comments) {
        if (comments.isEmpty()) {
            return List.of();
        }
        Set<Long> authorIds = comments.stream().map(PostComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> users = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return comments.stream()
                .map(c -> toCommentResponse(c, users.get(c.getUserId())))
                .toList();
    }

    private CommentResponse toCommentResponse(PostComment comment, User author) {
        return new CommentResponse(
                comment.getId(),
                comment.getUserId(),
                author == null ? null : author.getDisplayName(),
                author == null ? null : author.getAvatarUrl(),
                comment.getContent(),
                comment.getCreatedAt());
    }

    /** FR-015 / R5: validate gifUrl — HTTPS, host allowlist; null/blank → null. */
    private String validateGifUrl(String gifUrl) {
        if (gifUrl == null || gifUrl.isBlank()) {
            return null;
        }
        String trimmed = gifUrl.trim();
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "gifUrl không hợp lệ");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "gifUrl phải dùng HTTPS");
        }
        String host = uri.getHost();
        if (host == null || !GIF_HOST_ALLOWLIST.contains(host.toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "gifUrl chỉ chấp nhận tenor.com hoặc instagram.com");
        }
        // D1/I2: gif_url ≤500 ký tự — tránh lỗi DB 500 thay vì 400
        if (trimmed.length() > 500) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "gifUrl tối đa 500 ký tự");
        }
        return trimmed;
    }
}
