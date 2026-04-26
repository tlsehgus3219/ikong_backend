package com.ikongserver.entity;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "guardian_invitation")
public class GuardianInvitation {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String relation;

    private Boolean isPrimary = false;

    @Column(nullable = false)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void accept() {
        this.status = "ACCEPTED";
    }

    @Builder
    public GuardianInvitation(Users user, String phone, String name, String relation, Boolean isPrimary) {
        this.user = user;
        this.phone = phone;
        this.name = name;
        this.relation = relation;
        this.isPrimary = isPrimary != null ? isPrimary : false;
        this.status = "PENDING";
    }
}
