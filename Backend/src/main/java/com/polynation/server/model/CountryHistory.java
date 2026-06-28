package com.polynation.server.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "country_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Country country;

    @Column(name = "era_name")
    private String eraName;

    @Column(name = "start_year")
    private Integer startYear;

    @Column(name = "end_year")
    private Integer endYear;

    @Column(name = "flag_url")
    private String flagUrl;

    @Column(name = "emblem_url")
    private String emblemUrl;

    @Column(name = "borders_geometry", columnDefinition = "TEXT")
    private String bordersGeometry;
}
