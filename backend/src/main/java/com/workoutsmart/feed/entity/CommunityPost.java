package com.workoutsmart.feed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bài đăng cộng đồng — 018 social feed. media_url là key trên SeaweedFS. */
@Entity
@Table(name = "community_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "media_url")
    private String mediaUrl;

    /** image; null khi bài chỉ có text. */
    @Column(name = "media_type")
    private String mediaType;

    /** URL embed GIF (tenor.com/instagram.com) — nullable (V23, FR-015). */
    @Column(name = "gif_url")
    private String gifUrl;

    /** public | friends | private. */
    @Column(nullable = false)
    @Builder.Default
    private String audience = "public";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
