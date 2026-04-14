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
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "device_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @Column(unique = true, nullable = false)
    private String serialNum;
    private LocalDateTime lastConnectedAt;

    public void updateLastConnectedAt() {
        if (this.lastConnectedAt == null ||
            ChronoUnit.MINUTES.between(this.lastConnectedAt, LocalDateTime.now()) >= 1) {

            this.lastConnectedAt = LocalDateTime.now();
        }
    }

    @Builder
    public Device(Users user, String serialNum) {
        this.user = user;
        this.serialNum = serialNum;
        this.lastConnectedAt = LocalDateTime.now();
    }

}
