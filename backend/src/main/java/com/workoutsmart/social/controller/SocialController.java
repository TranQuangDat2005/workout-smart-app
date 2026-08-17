package com.workoutsmart.social.controller;

import com.workoutsmart.social.dto.ChallengeResponse;
import com.workoutsmart.social.dto.CreateChallengeRequest;
import com.workoutsmart.social.dto.FeedItemResponse;
import com.workoutsmart.social.dto.FriendshipRequest;
import com.workoutsmart.social.dto.FriendshipResponse;
import com.workoutsmart.social.dto.LeaderboardResponse;
import com.workoutsmart.social.dto.MessageResponse;
import com.workoutsmart.social.dto.UserSearchResponse;
import com.workoutsmart.social.service.SocialService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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

/** Controller mỏng — logic nằm ở SocialService. */
@RestController
@RequestMapping("/api/v1")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping("/users/search")
    public List<UserSearchResponse> searchUsers(Authentication auth, @RequestParam(defaultValue = "") String q) {
        return socialService.searchUsers(currentUserId(auth), q);
    }

    @PostMapping("/friendships")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendshipResponse sendRequest(Authentication auth, @Valid @RequestBody FriendshipRequest request) {
        return socialService.sendRequest(currentUserId(auth), request);
    }

    @GetMapping("/friendships/pending")
    public List<FriendshipResponse> pendingRequests(Authentication auth) {
        return socialService.pendingRequests(currentUserId(auth));
    }

    @PostMapping("/friendships/{id}/accept")
    public MessageResponse accept(Authentication auth, @PathVariable Long id) {
        return socialService.accept(currentUserId(auth), id);
    }

    @PostMapping("/friendships/{id}/reject")
    public MessageResponse reject(Authentication auth, @PathVariable Long id) {
        return socialService.reject(currentUserId(auth), id);
    }

    @DeleteMapping("/friendships/{id}")
    public MessageResponse unfriend(Authentication auth, @PathVariable Long id) {
        return socialService.unfriend(currentUserId(auth), id);
    }

    @GetMapping("/friends")
    public List<FriendshipResponse> friends(Authentication auth) {
        return socialService.friends(currentUserId(auth));
    }

    @GetMapping("/feed")
    public List<FeedItemResponse> feed(Authentication auth) {
        return socialService.feed(currentUserId(auth));
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardResponse> leaderboard() {
        return socialService.leaderboard();
    }

    @GetMapping("/challenges")
    public List<ChallengeResponse> challenges(Authentication auth) {
        return socialService.challenges(currentUserId(auth));
    }

    @GetMapping("/challenges/mine")
    public List<ChallengeResponse> myChallenges(Authentication auth) {
        return socialService.myChallenges(currentUserId(auth));
    }

    @PostMapping("/challenges")
    @ResponseStatus(HttpStatus.CREATED)
    public ChallengeResponse createChallenge(Authentication auth,
                                             @Valid @RequestBody CreateChallengeRequest request) {
        return socialService.createChallenge(currentUserId(auth), request);
    }

    @PostMapping("/challenges/{id}/join")
    public MessageResponse joinChallenge(Authentication auth, @PathVariable Long id) {
        return socialService.joinChallenge(currentUserId(auth), id);
    }

    private Long currentUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
