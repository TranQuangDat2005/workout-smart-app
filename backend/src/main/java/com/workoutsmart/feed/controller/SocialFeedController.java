package com.workoutsmart.feed.controller;

import com.workoutsmart.feed.dto.AddCommentRequest;
import com.workoutsmart.feed.dto.CommentResponse;
import com.workoutsmart.feed.dto.FeedPageResponse;
import com.workoutsmart.feed.dto.LikeResponse;
import com.workoutsmart.feed.dto.PostResponse;
import com.workoutsmart.feed.service.SeaweedStorageService;
import com.workoutsmart.feed.service.SocialFeedService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Controller mỏng cho feed cộng đồng (018) — logic nằm ở SocialFeedService. */
@RestController
@RequestMapping("/api/v1/feed")
public class SocialFeedController {

    private final SocialFeedService feedService;
    private final SeaweedStorageService storageService;

    public SocialFeedController(SocialFeedService feedService, SeaweedStorageService storageService) {
        this.feedService = feedService;
        this.storageService = storageService;
    }

    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(Authentication auth,
                                   @RequestParam(value = "content", required = false) String content,
                                   @RequestParam(value = "audience", defaultValue = "public") String audience,
                                   @RequestParam(value = "media", required = false) MultipartFile media,
                                   @RequestParam(value = "gifUrl", required = false) String gifUrl) {
        return feedService.createPost(currentUserId(auth), content, audience, media, gifUrl);
    }

    @GetMapping("/posts")
    public FeedPageResponse feed(Authentication auth,
                                 @RequestParam(defaultValue = "friends") String tab,
                                 @RequestParam(required = false) Long cursor) {
        return feedService.feed(currentUserId(auth), tab, cursor);
    }

    @PostMapping("/posts/{id}/like")
    public LikeResponse toggleLike(Authentication auth, @PathVariable Long id) {
        return feedService.toggleLike(currentUserId(auth), id);
    }

    @GetMapping("/posts/{id}/comments")
    public List<CommentResponse> comments(Authentication auth, @PathVariable Long id) {
        return feedService.comments(currentUserId(auth), id);
    }

    @PostMapping("/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(Authentication auth,
                                      @PathVariable Long id,
                                      @Valid @RequestBody AddCommentRequest request) {
        return feedService.addComment(currentUserId(auth), id, request.content());
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(Authentication auth, @PathVariable Long id) {
        feedService.deletePost(currentUserId(auth), id);
    }

    /** Endpoint public để browser tải ảnh mà không cần gửi Authorization. */
    @GetMapping("/media/**")
    public ResponseEntity<byte[]> media(HttpServletRequest request) {
        String key = request.getRequestURI().substring("/api/v1/feed/media/".length());
        SeaweedStorageService.MediaContent media = storageService.fetch(key);
        return ResponseEntity.status(media.statusCode())
                .contentType(MediaType.parseMediaType(media.contentType()))
                .body(media.body());
    }

    private Long currentUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
