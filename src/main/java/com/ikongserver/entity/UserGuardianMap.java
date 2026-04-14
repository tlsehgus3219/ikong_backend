package com.ikongserver.entity;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "user_guardian_map")
public class UserGuardianMap {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id")
    private Guardian guardian;

    private String relation;

    private String isPrimary;

    private String isActive;

    public void updateIsPrimary(String isPrimary) {
        this.isPrimary = isPrimary;
    }

    public void updateIsActive(String isActive) {
        this.isActive = isActive;
    }

    public void updateRelation(String relation) {
        this.relation = relation;
    }

    @Builder
    public UserGuardianMap(String relation, String isPrimary, String isActive, Users user, Guardian guardian) {
        this.user = user;
        this.guardian = guardian;
        this.relation = relation;
        this.isPrimary = isPrimary;
        this.isActive = isActive;
    }
}
