package com.polynation.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "countries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "capital")
    private String capital;

    @Column(name = "capital_lat")
    private Double capitalLat;

    @Column(name = "capital_lng")
    private Double capitalLng;

    @Column(name = "history_info", columnDefinition = "TEXT")
    private String historyInfo;

    @Column(name = "culture_info", columnDefinition = "TEXT")
    private String cultureInfo;

    @Column(name = "music_info", columnDefinition = "TEXT")
    private String musicInfo;

    @Column(name = "movies_info", columnDefinition = "TEXT")
    private String moviesInfo;

    @Column(name = "sports_info", columnDefinition = "TEXT")
    private String sportsInfo;

    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Visit> visits = new ArrayList<>();

    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<CountryHistory> history = new ArrayList<>();
}