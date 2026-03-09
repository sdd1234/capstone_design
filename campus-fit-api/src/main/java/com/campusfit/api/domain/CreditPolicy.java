package com.campusfit.api.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "credit_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preference_id", nullable = false, unique = true)
    private TimetablePreference preference;

    private Integer minCredits;
    private Integer maxCredits;
    private Integer targetCredits;
}
