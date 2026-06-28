package com.polynation.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "visit_point_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitPointImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_point_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VisitPoint visitPoint;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) uploadedAt = LocalDateTime.now();
    }
}
