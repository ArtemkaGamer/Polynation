package com.polynation.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "user_achievements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "achievement_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "achievement_id", nullable = false)
    private Long achievementId;

    @Column(name = "awarded_at")
    private LocalDateTime awardedAt;

    @PrePersist
    protected void onCreate() {
        if (awardedAt == null) awardedAt = LocalDateTime.now();
    }
}
