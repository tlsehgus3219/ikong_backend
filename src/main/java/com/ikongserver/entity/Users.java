package com.ikongserver.entity;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(nullable = false)
    private String name;

    private String phone;
    private LocalDate birthDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private String socialId;

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updatePhone(String phone) {
        this.phone = phone;
    }

    public void updateBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public void updateFromKakao(String name, String phone, LocalDate birthDate) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
        if (birthDate != null) this.birthDate = birthDate;
    }

    @Builder
    public Users(String email, String password, String name, String phone, LocalDate birthDate, String socialId) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.socialId = socialId;
    }

}
