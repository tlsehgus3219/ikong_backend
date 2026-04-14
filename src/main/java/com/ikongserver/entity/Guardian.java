package com.ikongserver.entity;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "guardian")
public class Guardian {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "guardian_id")
    private Long id;

    private String password;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String socialId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updatePhone(String phone) {
        this.phone = phone;
    }

    @Builder
    public Guardian(String password, String name, String phone, String socialId) {
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.socialId = socialId;
    }



}
